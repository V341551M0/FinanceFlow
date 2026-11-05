package com.financeflow.investment;

import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.investment.dto.*;
import com.financeflow.market.MarketDataProvider;
import com.financeflow.market.Quote;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PortfolioService {

    private final InvestmentRepository investments;
    private final InvestmentOperationRepository operations;
    private final AssetRepository assets;
    private final UserRepository users;
    private final MarketDataProvider marketData;

    public PortfolioService(InvestmentRepository investments,
                            InvestmentOperationRepository operations,
                            AssetRepository assets,
                            UserRepository users,
                            MarketDataProvider marketData) {
        this.investments = investments;
        this.operations = operations;
        this.assets = assets;
        this.users = users;
        this.marketData = marketData;
    }

    @Transactional
    public PositionResponse create(Long userId, CreateInvestmentRequest req) {
        var user = users.findById(userId).orElseThrow();

        var asset = assets.findBySymbolIgnoreCaseAndExchange(req.symbol(), req.exchange())
                .orElseGet(() -> assets.save(Asset.builder()
                        .symbol(req.symbol().toUpperCase())
                        .name(req.name())
                        .type(req.type())
                        .exchange(req.exchange())
                        .currency(req.currency().toUpperCase())
                        .build()));

        var investment = investments.findByUserIdAndAssetId(userId, asset.getId())
                .orElseGet(() -> investments.save(Investment.builder()
                        .user(user)
                        .asset(asset)
                        .quantity(BigDecimal.ZERO)
                        .averagePrice(BigDecimal.ZERO)
                        .build()));

        var op = InvestmentOperation.builder()
                .investment(investment)
                .type(OperationType.BUY)
                .quantity(req.quantity())
                .price(req.price())
                .operationDate(req.operationDate())
                .fees(req.fees() == null ? BigDecimal.ZERO : req.fees())
                .build();
        operations.save(op);

        recalculatePosition(investment);
        return toResponse(investment);
    }

    @Transactional
    public PositionResponse addOperation(Long userId, Long investmentId, AddOperationRequest req) {
        var investment = investments.findByIdAndUserId(investmentId, userId)
                .orElseThrow(() -> new NotFoundException("Investimento não encontrado"));

        if (req.type() == OperationType.SELL
                && req.quantity().compareTo(investment.getQuantity()) > 0) {
            throw new BusinessException("Quantidade a vender maior que a posição atual.");
        }

        var op = InvestmentOperation.builder()
                .investment(investment)
                .type(req.type())
                .quantity(req.quantity())
                .price(req.price())
                .operationDate(req.operationDate())
                .fees(req.fees() == null ? BigDecimal.ZERO : req.fees())
                .notes(req.notes())
                .build();
        operations.save(op);

        recalculatePosition(investment);
        return toResponse(investment);
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> listPositions(Long userId) {
        return investments.findAllByUserIdOrderByAssetSymbolAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvestmentOperation> listOperations(Long userId, Long investmentId) {
        investments.findByIdAndUserId(investmentId, userId)
                .orElseThrow(() -> new NotFoundException("Investimento não encontrado"));
        return operations.findAllByInvestmentIdOrderByOperationDateDesc(investmentId);
    }

    @Transactional
    public void delete(Long userId, Long investmentId) {
        var inv = investments.findByIdAndUserId(investmentId, userId)
                .orElseThrow(() -> new NotFoundException("Investimento não encontrado"));
        investments.delete(inv);
    }

    /**
     * Preço médio ponderado:
     *  - BUY:  avg' = (qty * avg + buyQty * buyPrice + fees) / (qty + buyQty)
     *  - SELL: avg' = avg (não altera), qty' = qty - sellQty
     */
    private void recalculatePosition(Investment investment) {
        var ops = operations.findAllByInvestmentIdOrderByOperationDateDesc(investment.getId());
        // Recalcula do zero para garantir precisão histórica
        var allOps = ops.reversed();
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal avg = BigDecimal.ZERO;

        for (var op : allOps) {
            if (op.getType() == OperationType.BUY) {
                var newQty = qty.add(op.getQuantity());
                var cost = qty.multiply(avg)
                        .add(op.getQuantity().multiply(op.getPrice()))
                        .add(op.getFees());
                avg = newQty.signum() == 0 ? BigDecimal.ZERO
                        : cost.divide(newQty, 8, RoundingMode.HALF_UP);
                qty = newQty;
            } else { // SELL
                qty = qty.subtract(op.getQuantity());
                if (qty.signum() == 0) avg = BigDecimal.ZERO;
            }
        }
        investment.setQuantity(qty);
        investment.setAveragePrice(avg);
    }

    private PositionResponse toResponse(Investment inv) {
        var asset = inv.getAsset();
        Quote quote = marketData.getQuote(asset.getSymbol(), asset.getType(), asset.getExchange());

        var invested = inv.getQuantity().multiply(inv.getAveragePrice());
        var currentPrice = quote.price();
        var marketValue = currentPrice == null ? null : inv.getQuantity().multiply(currentPrice);
        var profitLoss = marketValue == null ? null : marketValue.subtract(invested);
        var profitLossPct = profitLoss == null || invested.signum() == 0 ? null
                : profitLoss.multiply(BigDecimal.valueOf(100))
                .divide(invested, 2, RoundingMode.HALF_UP);

        return new PositionResponse(
                inv.getId(), asset.getId(), asset.getSymbol(), asset.getName(),
                asset.getType(), asset.getExchange(), asset.getCurrency(),
                inv.getQuantity(), inv.getAveragePrice(), currentPrice,
                invested, marketValue, profitLoss, profitLossPct,
                quote.source(), quote.asOf());
    }
}
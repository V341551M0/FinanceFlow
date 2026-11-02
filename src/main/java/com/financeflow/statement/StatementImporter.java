package com.financeflow.statement;

import java.io.IOException;
import java.io.InputStream;

/**
 * Contrato extensível para importadores de extratos.
 * Novos formatos (OFX, QIF, PDF) implementam esta interface sem alterar o serviço principal.
 */
public interface StatementImporter {
    StatementFormat format();
    ParsedStatement parse(InputStream in, Long accountId) throws IOException;
}
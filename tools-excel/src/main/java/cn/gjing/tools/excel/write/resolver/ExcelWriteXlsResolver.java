package cn.gjing.tools.excel.write.resolver;

import cn.gjing.tools.excel.metadata.resolver.ExcelWriterResolver;
import cn.gjing.tools.excel.write.ExcelWriterContext;
import cn.gjing.tools.excel.write.callback.ExcelAutoMergeCallback;

import java.util.List;
import java.util.Map;

/**
 * XLS resolver
 *
 * @author Gjing
 **/
class ExcelWriteXlsResolver extends ExcelWriterResolver {
    private final ExcelWriteExecutor excelWriteExecutor;

    public ExcelWriteXlsResolver(ExcelWriterContext context) {
        super(context);
        this.excelWriteExecutor = new ExcelWriteExecutor(context);
    }

    @Override
    public ExcelWriterResolver writeHead(boolean needHead, Map<String, String[]> dropdownBoxValues) {
        this.excelWriteExecutor.writeHead(needHead, dropdownBoxValues);
        return this;
    }

    @Override
    public ExcelWriterResolver simpleWriteHead(boolean needHead) {
        this.excelWriteExecutor.simpleWriteHead(needHead);
        return this;
    }

    @Override
    public void simpleWrite(List<List<Object>> data, boolean mergeEmpty,Map<String, ExcelAutoMergeCallback<?>> callbackCache) {
        this.excelWriteExecutor.simpleWriteBody(data, mergeEmpty,callbackCache);
    }

    @Override
    public void write(List<?> data) {
        this.excelWriteExecutor.writeBody(data);
    }
}

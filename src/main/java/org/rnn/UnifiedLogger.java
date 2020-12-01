package org.rnn;

public interface UnifiedLogger
{
    void info(String str);
    void error(String err);
    void warn(String warn);
    void end();
}

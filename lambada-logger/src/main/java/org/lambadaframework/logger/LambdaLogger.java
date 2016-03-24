package org.lambadaframework.logger;


import com.amazonaws.services.lambda.runtime.log4j.LambdaAppender;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

public class LambdaLogger {

    private static Appender appender;

    private static Appender getAppender() {
        if (appender == null) {
            PatternLayout patternLayout = new PatternLayout();
            patternLayout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss} <%X{AWSRequestId}> %-5p %c{1}:%L - %m%n");
            appender = new LambdaAppender();
            appender.setLayout(patternLayout);
        }
        return appender;
    }

    public static org.apache.log4j.Logger getLogger(Class clazz) {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(clazz);
        logger.setLevel(Level.ERROR);
        logger.addAppender(getAppender());
        return logger;
    }
}

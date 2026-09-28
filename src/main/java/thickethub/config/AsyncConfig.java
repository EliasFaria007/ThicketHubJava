package thickethub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor padrão para @Async — Virtual Threads do Java 21.
     * Uma thread virtual por tarefa: barato de criar, sem pool dimensionado à mão.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Executor dedicado para tarefas críticas (ex.: envio de SMS),
     * com nome de thread para rastreio em logs.
     */
    @Bean(name = "smsExecutor")
    public Executor smsExecutor() {
        return new ThreadPoolTaskExecutor() {{
            setThreadFactory(Thread.ofVirtual().name("sms-", 0).factory());
            setCorePoolSize(Integer.MAX_VALUE);   // sem limite: virtual threads são baratas
            setMaxPoolSize(Integer.MAX_VALUE);
        }};
    }
}
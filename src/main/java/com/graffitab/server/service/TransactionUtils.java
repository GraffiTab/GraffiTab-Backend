package com.graffitab.server.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TransactionUtils {

	private static Logger LOG = LogManager.getLogger();

	@Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate requiresNewTransactionTemplate;

    private TransactionTemplate transactionTemplate;

	private ExecutorService postTransactionCommitActionsExecutor = Executors.newFixedThreadPool(2);

	private static final ThreadLocal<List<Runnable>> RUNNABLES_BY_THREAD = new ThreadLocal<List<Runnable>>() {
		@Override
		protected List<Runnable> initialValue() {
			return new ArrayList<>();
		}
	};

	@PostConstruct
    public void init() throws Exception {
        requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        requiresNewTransactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

	public void executeInTransaction(Runnable runnable) {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
	        @Override
	        protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
	            runnable.run();
	        }
	    });
	}

	public <T> T executeInTransactionWithResult(Callable<T> callableWithResult) {
		T result = transactionTemplate.execute(new TransactionCallback<T>() {

			@Override
			public T doInTransaction(TransactionStatus status) {
				try  {
					return callableWithResult.call();
				} catch (Throwable t) {
					LOG.error("Error executing transaction with result", t);
					throw new RuntimeException(t);
				}
			}
		});

		executePostTransactionCommitActions();

		return result;
	}

	public void executeInNewTransaction(Runnable runnable) {
		requiresNewTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
	        @Override
	        protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
	            runnable.run();
	        }
	    });
	}

	public void addPostTransactionCommitAction(Runnable runnable) {
		RUNNABLES_BY_THREAD.get().add(runnable);
	}

	private void executePostTransactionCommitActions() {
		RUNNABLES_BY_THREAD.get().forEach((runnable) -> {
			postTransactionCommitActionsExecutor.submit(runnable);
		});

		RUNNABLES_BY_THREAD.get().clear();
	}
}

// This has a problem sometimes when doing an insert to the database. Need to investigate.
//		return transactionTemplate.execute((transactionStatus) -> {
//			try  {
//				return callableWithResult.call();
//			} catch (Throwable t) {
//				LOG.error("Error executing transaction with result", t);
//				throw new RuntimeException(t);
//			}
//		});

package com.graffitab.server.test.api;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.IsolationLevelDataSourceAdapter;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.TransactionDefinition;

import java.util.Properties;

@Configuration
@Profile("unit-test")
@ImportResource({"classpath:jdbc-test.xml"})
public class TestDatabaseConfig {

	// Read from XML
		@Autowired
		private ComboPooledDataSource targetDataSource;

		@Bean
		public HibernateTransactionManager transactionManager() {
			HibernateTransactionManager transactionManager = new HibernateTransactionManager();
			transactionManager.setSessionFactory(sessionFactory().getObject());
			return transactionManager;
		}

		public IsolationLevelDataSourceAdapter dataSource() {
			IsolationLevelDataSourceAdapter dataSource = new IsolationLevelDataSourceAdapter();
			dataSource.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
			dataSource.setTargetDataSource(targetDataSource);
			return dataSource;
		}

		@Bean
	    public LocalSessionFactoryBean sessionFactory() {

	        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
	        sessionFactory.setDataSource(dataSource());
	        sessionFactory.setPackagesToScan("com.graffitab.server.persistence.model");
	        sessionFactory.setHibernateProperties(hibernateProperties());
	        return sessionFactory;
	    }

		public Properties hibernateProperties() {
	        return new Properties() {
				private static final long serialVersionUID = 1L;

				{
	                setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
	                setProperty("hibernate.show_sql", "false");
	                setProperty("hibernate.hbm2ddl.auto", "update");
	            }
	        };
	    }
}

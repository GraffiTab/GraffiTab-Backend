package com.graffitab.server.config.spring;

import java.util.Properties;

import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.IsolationLevelDataSourceAdapter;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Configuration
@Profile("main")
public class MainDatabaseConfig {

	private static Logger LOG = LogManager.getLogger();

	@Resource
	private Environment env;

	private String jdbcUrl;

	private String dbUsername;

	private String dbPassword;

	private String dbHost;

	private Integer dbPort;

	private Integer dbBoneCpPartitionCount;

	private Integer dbBoneCpMinConnectionsPerPartition;

	private Integer dbBoneCpMaxConnectionsPerPartition;

	private Integer dbBoneCpAcquireIncrement;

	private Integer dbBoneCpIdleMaxAgeMinutes;

	private Integer dbBoneCpIdleConnectionTestPeriodInMinutes;

	@PostConstruct
	public void init() {
		LOG.info("*** Loading environment properties...");
		readEnvironmentProperties();
	}

	@Bean
	public HibernateTransactionManager transactionManager() {
		HibernateTransactionManager transactionManager = new HibernateTransactionManager();
		transactionManager.setSessionFactory(sessionFactory().getObject());
		return transactionManager;
	}

	private IsolationLevelDataSourceAdapter dataSource() {
		IsolationLevelDataSourceAdapter dataSource = new IsolationLevelDataSourceAdapter();
		dataSource.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		dataSource.setTargetDataSource(targetDataSource());
		return dataSource;
	}

	@Bean
	public BoneCPDataSource targetDataSource() {

		LOG.info("*** [BoneCP] Database configuration read from application properties: \n" +
				"**** jbcUrl -> " + jdbcUrl + "\n" +
				"**** user   -> " + dbUsername + "\n" +
				"**** dbHost -> " + dbHost + "\n" +
				"**** dbPort -> " + dbPort);
		// http://kielczewski.eu/2014/05/database-connection-pooling-with-bonecp-in-spring-boot-application/
		BoneCPDataSource dataSource = new BoneCPDataSource();
		dataSource.setDriverClass("com.mysql.jdbc.Driver");
		dataSource.setJdbcUrl(jdbcUrl);
		dataSource.setUsername(dbUsername);
		dataSource.setPassword(dbPassword);
		dataSource.setIdleConnectionTestPeriodInMinutes(dbBoneCpIdleConnectionTestPeriodInMinutes);
		dataSource.setIdleMaxAgeInMinutes(dbBoneCpIdleMaxAgeMinutes);
		dataSource.setMaxConnectionsPerPartition(dbBoneCpMaxConnectionsPerPartition);
		dataSource.setMinConnectionsPerPartition(dbBoneCpMinConnectionsPerPartition);
		dataSource.setPartitionCount(dbBoneCpPartitionCount);
		dataSource.setAcquireIncrement(dbBoneCpAcquireIncrement);
		dataSource.setStatementsCacheSize(100);
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

	private Properties hibernateProperties() {
        return new Properties() {
			private static final long serialVersionUID = 1L;
			{
                setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLInnoDBDialect");
                setProperty("hibernate.show_sql", "false");
                setProperty("hibernate.hbm2ddl.auto", "validate");
            }
        };
	}

	private void readEnvironmentProperties() {
		this.jdbcUrl = env.getProperty("db.jdbcUrl","");

		if (this.jdbcUrl.contains("$")) {
			//TODO: complete validation
			throw new IllegalArgumentException("JDBC url contains a placeholder " + this.jdbcUrl);
		}

		this.dbUsername = env.getProperty("db.username","");
		this.dbPassword = env.getProperty("db.password","");
		this.dbHost = env.getProperty("db.host","localhost");
		this.dbPort = Integer.parseInt(env.getProperty("db.port","3306"));

		// BoneCP
		this.dbBoneCpPartitionCount = Integer.parseInt(env.getProperty("db.partitionCount","4"));
		this.dbBoneCpMinConnectionsPerPartition =  Integer.parseInt(env.getProperty("db.minConnectionsPerPartition","5"));
		this.dbBoneCpMaxConnectionsPerPartition = Integer.parseInt(env.getProperty("db.maxConnectionsPerPartition","15"));
		this.dbBoneCpAcquireIncrement = Integer.parseInt(env.getProperty("db.acquireIncrement","5"));
		this.dbBoneCpIdleMaxAgeMinutes = Integer.parseInt(env.getProperty("db.idleMaxAgeMinutes","240"));
		this.dbBoneCpIdleConnectionTestPeriodInMinutes = Integer.parseInt(env.getProperty("db.idleConnectionTestPeriodMinutes","60"));
	}
}

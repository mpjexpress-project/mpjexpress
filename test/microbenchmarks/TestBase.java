package microbenchmarks;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public abstract class TestBase implements Runnable {
	private String testName;
	private Properties testProperties;
	private String[] osArgs;

	private void loadProperties() {
		this.testName = this.getClass().getSimpleName();
		InputStream stream = getClass().getClassLoader().getResourceAsStream(testName + "Config.properties");
		try {
			if (stream != null) {
				testProperties = new Properties();
				testProperties.load(stream);
			} else {
				throw new FileNotFoundException(testName + "Config.properties file was not found.");
			}
		} catch (Exception e) {
			testProperties = System.getProperties();
		}
	}

	private Reporter reporter;

	protected abstract void initialize(String[] args);

	protected abstract void runTest();

	protected abstract void cleanUp();

	public Reporter getReporter() {
		return reporter;
	}

	public TestBase() {
		reporter = new Reporter();
	}

	public String getTestName() {
		return testName;
	}

	public Properties getProperties() {
		return testProperties;
	}

	public String[] getOsArgs() {
		return osArgs;
	}

	public void setOsArgs(String[] osArgs) {
		this.osArgs = osArgs;
	}

	public void printReport() {
		reporter.print();
	}

	@Override
	public void run() {
		loadProperties();
		initialize(osArgs);
		runTest();
		cleanUp();
	}
}

package eapli.aisafe;

public class Application {

	public static final String VERSION = "1.0.0";
	public static final String COPYRIGHT = "(C) 2025, AISafe Team";

	private static final AppSettings SETTINGS = new AppSettings();

	public static AppSettings settings() {
		return SETTINGS;
	}

	private Application() {
	}
}

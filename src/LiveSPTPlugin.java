import org.micromanager.Studio;
import org.micromanager.data.ProcessorPlugin;
import org.micromanager.PropertyMap;
import org.micromanager.data.ProcessorConfigurator;
import org.micromanager.data.ProcessorFactory;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;



@Plugin(type = ProcessorPlugin.class)
public class LiveSPTPlugin implements ProcessorPlugin, SciJavaPlugin {
	public static final String menuName = "LiveSPT";
	private Studio app;
  // Il manque sûrement des attributs
	
	
	@Override
	public ProcessorConfigurator createConfigurator(PropertyMap settings) {
		// Mettre des trucs ici
	}
	
	@Override
	public ProcessorFactory createFactory(PropertyMap settings) {
		// Mettre des trucs là
	}
	
	@Override
	public void setContext(Studio studio) {
		app = studio;
	}
	
	@Override
    public String getName() {
        return menuName;
    }

    @Override
    public String getHelpText() {
        return ""; // Je sais pas quoi mettre
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public String getCopyright() {
        return ""; // Je sais pas quoi mettre non plus
    }
}

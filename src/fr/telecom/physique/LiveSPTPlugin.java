// Projet LiveSPT
// Auteurs : Yzouille, Kimvitesse, Sananas03, Poissondavril03, FavreIndustries
// Date de création : 03/12/2024
// Date de modification : 21/01/2025
// Version : 0.3

// Fichier de départ pour créer un plugin MicroManager
// Les @Override correspondent aux méthodes des interfaces à redéfinir obligatoirement
// C'est une ébauche, il y a sûrement des modifications à faire

package fr.telecom.physique;

import org.micromanager.Studio;
import org.micromanager.data.ProcessorPlugin;
import org.micromanager.PropertyMap;
import org.micromanager.data.ProcessorConfigurator;
import org.micromanager.data.ProcessorFactory;
import org.scijava.plugin.Plugin;


@Plugin(type = ProcessorPlugin.class)
public class LiveSPTPlugin implements ProcessorPlugin {
	public static final String menuName = "LiveSPT";
	private Studio app;
	// Il manque sûrement des attributs
	
	
	@Override
	public ProcessorConfigurator createConfigurator(PropertyMap settings) {
		return new LiveSPTConfigurator(settings);
	}
	
	@Override
	public ProcessorFactory createFactory(PropertyMap settings) {
		return new LiveSPTFactory(settings, app);
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

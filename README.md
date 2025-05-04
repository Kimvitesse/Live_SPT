# Live Single Particle Tracking

Live_SPTracking is a Micro-Manager plugin that can track particles in 3D on live mode within a selected area.

## Installation

Download the file [LiveSPTPlugin.jar]() in the *MMPlugins* folder to install the plugin on Micro-Manager.

```bash
LiveSPTPlugin.jar
```

## Usage

Open the plugin on Micro-Manager by searching in *Plugin > On the fly image processing*.


Initialization of the piezoelectric stage:
Write a BeanShell script in *Tools/Script Panel*, click on a*dd* then *run* to launch the script.

Control of the piezoelectric stage and motorized stage in the Stage Control window:
click on *Stage*

Starting the live view:
*Live*, *auto once* (adjusts contrast and gain)

Plugin parameter settings:
Enter the parameters in the corresponding window using the *configure* button.

Plugin tests:
Set the piezoelectric stage to 0 and move the motorized stage so that the particles are visible, then start the live view.


## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

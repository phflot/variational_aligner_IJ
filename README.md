
# ImageJ and Fiji Plugin for fast, variational 1D alignment of multichannel linescan data

This is an ImageJ and Fiji plugin that can be used for the variational alignment of displacement jitter in linescan recordings. The plugin supports the simultaneous, weighted registration of multiple channels.

The estimated displacements have native subpixel precision and can compensate non-constant displacements. The preprint of our paper can be found on [bioRxiv](https://www.biorxiv.org/content/10.1101/2020.06.27.151522v1) and the project website [here](https://phflot.github.io/variational_aligner/).

![](img.jpg?raw=true "Title")

## Download

Download the repository via
```
$ git clone https://github.com/phflot/variational_aligner_IJ.git
```

## Documentation and Usage

The plugin can be compiled with maven and then added to ImageJ and Fiji via install plugin. The class ```OptionsDialog``` can be run as standalone java application which loads example data into ImageJ and starts the plugin. 

## Citation

The code is based on a publication. If you use this plugin for your work, please cite: 
  
> P. Flotho, D. Thinnes, B. Kuhn, C. J. Roome, J. F. Vibell and D. J. Strauss, “Fast Variational Alignment of non-flat 1D Displacements with Applications in Neuroimaging” (bioRxiv), 2020. 

BibTeX entry
```
@article{floth20,
    author = {Flotho, P. and Thinnes, D. and Kuhn, B. and Roome, C. J. and Vibell, J. F. and Strauss, D. J.},
    title = {Fast Variational Alignment of non-flat 1D Displacements for Applications in Neuroimaging},
	elocation-id = {2020.06.27.151522},
	year = {2020},
	doi = {10.1101/2020.06.27.151522},
	publisher = {Cold Spring Harbor Laboratory},
	URL = {https://www.biorxiv.org/content/early/2020/06/29/2020.06.27.151522},
	eprint = {https://www.biorxiv.org/content/early/2020/06/29/2020.06.27.151522.full.pdf},
	journal = {bioRxiv}
}
```

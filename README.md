
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
  
> Flotho, P., Thinnes, D., Kuhn, B., Roome, C. J., Vibell, J. F., & Strauss, D. J. (2021). Fast variational alignment of non-flat 1D displacements for applications in neuroimaging. Journal of Neuroscience Methods, 353, 109076.

BibTeX entry
```
@article{floth21,
  title={Fast variational alignment of non-flat 1D displacements for applications in neuroimaging},
  author={Flotho, Philipp and Thinnes, David and Kuhn, Bernd and Roome, Christopher J and Vibell, Jonas F and Strauss, Daniel J},
  journal = {Journal of Neuroscience Methods},
  volume = {353},
  pages = {109076},
  year = {2021},
  issn = {0165-0270},
  doi = {https://doi.org/10.1016/j.jneumeth.2021.109076},
  url = {https://www.sciencedirect.com/science/article/pii/S016502702100011X},
}
```

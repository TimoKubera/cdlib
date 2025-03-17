import dhl.multicontainer.Image

Image call(Map config) {
	Image image
	if (config.containsKey('alwaysPull')) {
		image = new Image(config.alias, config.imageName, config.alwaysPull)
	} else {
		image = new Image(config.alias, config.imageName)
	}
	
	return image
}

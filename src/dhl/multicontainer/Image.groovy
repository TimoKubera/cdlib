package dhl.multicontainer

class Image implements Serializable {
	final String alias
	final String imageName
	final boolean alwaysPull
	
	Image(alias, imageName, alwaysPull = false) {
		this.alias = alias
		this.imageName = imageName
		this.alwaysPull = alwaysPull
	}
}

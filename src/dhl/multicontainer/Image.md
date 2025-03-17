# Image
This class is a value object (read: struct) for storing image information about one image instance.

## Configuration
| Name | Description | Format |
| -----| ------------ | ------ |
| alias | name of the container spawned for the image | String |
| imageName | image reference name in the image registry | String |
| alwaysPull | option to pull the newest image before use | Boolean |
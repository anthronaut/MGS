# MGS
## License
This software is licensed using the MIT Software License (see file [LICENSE](https://github.com/musethno/MGS/blob/master/LICENSE) for Details). In short, this means, the MGS is obtainable for free and you are free to do anything you want with it, as long as you include the copyright notice.

## System Requirements
The MGS runs on the [Play Framework](https://www.playframework.com/). Please refer to the [Play Documentation](https://www.playframework.com/documentation/2.4.x/Home) for more details on installation and usage in other setups than described here.

### Server
- The MGS is java based and is therefore cross-platform in theory. The current version has been tested on Debian GNU/Linux 8 "Jessie" only.
- Play runs an own web server that listens on a given port (default: 9000). You can use a front-end server for proxying/load balancing. The current version works fine in combination with Apache 2.4.10.

#### Play Framework dependencies
- The Play Framework, current version tested with Play 2.4.6 ("Damiya")
- JDK 8+

## Getting Started
### Installation
1. Navigate to the installation directory of your choice. If you install the application outside your /home directory, be sure to fix the file permissions accordingly.
2. Clone this repository: ```git clone https://github.com/musethno/MGS.git .```
3. Run the application with: ```./activator play start```
4. The MGS compiles and starts the web server, listening by default to 9000
5. In your browser, navigate to [localhost:9000](http://localhost:9000)

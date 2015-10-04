#Jaywixz
Jaywixz is an offline [Wikipedia](https://wikipedia.org) reader inspired by [Kiwix](http://kiwix.org). Instead of using [ZIM](http://openzim.org), content archives are in the standard [tar](https://en.wikipedia.org/wiki/Tar_(computing)) archive file format and compressed with [xz](http://tukaani.org/xz/). The mobile-friendly web frontend provides a familiar search engine experience, powered by Twitter [typeahead.js](https://github.com/twitter/typeahead.js). The web backend is built using Apache [Lucene](https://lucene.apache.org/) and [Vert.x](http://vertx.io/), serving content over a local private network.

Jaywixz: <b>J</b>ava, <b>Wi</b>kipedia, <b>xz</b>.

**Minimum Requirements**
**Software:**
[Apache Maven](https://maven.apache.org/)
Java v1.7
**Hardware:**
512MB RAM (Works on a Raspberry Pi Model B)

**How to install and run Jaywixz**
Jaywixz uses standard Java development tools.
```
$ git clone https://github.com/cody271/jaywixz
$ cd jaywixz/
$ mvn compile package
$ java -jar target/jaywixz-0.1.jar /path/to/archive.txz
```
Using your preferred browser, navigate to [localhost:9090](http://localhost:9090)
or
From a device on the same network, navigate to your.local.ip.addr:9090

Enjoy your local copy of Wikipedia!

![](https://raw.githubusercontent.com/cody271/jaywixz/master/screenshots/safari%20v8.0.6%202%202015-09-28.png)

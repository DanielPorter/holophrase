# Holophrase: Baby's First DSL
Holophrase is a toy DSL (domain specific language) for writing declarative infrastructure. It only supports one use case - creating a VM, installing SBT, and then cloning and building a scala project from github.
 
It's a fairly minimal end-to-end example of a compiled language. It compiles down to bash and python. It also has an interpreter - technically a "language runtime". As a byproduct of writing the compiler, I also created an embedded DSL.

Holophrase only supports one use case: start a VM, install SBT on it, and then clone and build a scala project from github. 

The goals of the talk are to 
1. Show you how to write an embedded domain specific language using case classes,
2. Convince you that it really only sounds hard,
3. Help you understand end-to-end what a compiler does, and perhaps allay some of the blind terror you might feel about them, and
4. point at some of the falinings of case-classes-as-DSLs, and some of the techniques that build on them.

Aspirationally, the talk will give you another lens through which to view the motivations and techniques for functional programming.

The language and the talk are heavily inspired by James Earl Douglas' talk on NixOps at Lambda Conf earlier this year. Particular thanks to the folks at Underscore for so graciously helping my plodding ascent up the ladder of abstraction.

## Setup

 clone this repo, then acquire the api key:
```bash
git clone https://github.com/danielporter/holophrase
cd holophrase
curl 165.227.103.243/doapikey > doapikey
```

For all of this to work right you need
1. bash
2. openssl
3. sbt

I think that's all. I tested it on my macbook and on ubuntu 14.04.
## Usage
To stop all of your servers:
```bash
sbt "run stopall"
```
Look at the server definition in examples/server.holophrase
```bash
cat examples/server.holophrase
```
To use the interpreter to spin up a server, run
```bash
sbt "run start examples/server.holophrase"
```

To kill a server, run
```bash
sbt "run stop examples/server.holophrase"
```

To do error checking on a server definition, run
```bash
sbt run validate examples/invalidserver.holophrase
```

### Embedded DSL
To use the embedded DSL, start the scala console:
```bash
sbt console
```

Define a server:
```scala
import holophrase._
val server = DigitalOceanServer("myfirstserver", ScalaRepo("https://github.com/danielporter/holophrase.git", SBTCommand("compile")))
//server: holophrase.DigitalOceanServer = DigitalOceanServer(myfirstserver,ScalaRepo(https://github.com/danielporter/holophrase.git,SBTCommand(compile)))
```

Validate the parameters:
```scala
server.validate
```
Start it:
```scala
server.run()
```

Then destroy it:
```scala
server.destroy
```

Compile it to bash, if you're feeling lucky. The code probably doesn't work. Bash is a very special language.
```scala
server.compile.mkString("\n")
```


### The parser

There's a parser combinator displayed in the talk. 
```scala
holophrasedemoparser.run("Server(name=\"myserver\", repo=\"https://github.com/slamdata/matryoshka.git\", command=\"compile\")")
// res8: holophrase.DigitalOceanServer = DigitalOceanServer(myserver,ScalaRepo(https://github.com/slamdata/matryoshka.git,SBTCommand(compile)))

```

Give it some bad input. I did a really lazy implementation so it blows up, and with a surprisingly helpful error message:
```scala
holophrasedemoparser.run( "Server(name=\"myserver\", repost=\"https://github.com/slamdata/matryoshka\", command=\"build\")")
scala.MatchError: [1.29] failure: `=' expected but `s' found

Server(name="myserver", repost="https://github.com/slamdata/matryoshka", command="build")
                            ^ (of class scala.util.parsing.combinator.Parsers$Failure)
```
It's accidentally helpful because the language specified by the parser is so stupidly specific - the language syntax defines "Server", which is followed by an lparen, and then three specific keyword/string literal pairs.

There's also a slightly more sophisticated parser. It demonstrates "tokenization", wherein you parse a string and things like string literals, parentheses, and language keywords. 

```scala
holophraseparser.run("Server(name=\"myserver\", repost=\"https://github.com/slamdata/matryoshka\", command=\"build\")")
// res10: holophrase.HolophraseAST = Server(List(Assignment(Identifier(name),StringLiteral("myserver")), Assignment(Identifier(repost),StringLiteral("https://github.com/slamdata/matryoshka")), Assignment(Identifier(command),StringLiteral("build"))))
```

This doesn't say yea or nay, it just parses the string. I tried to make sure it would tokenize *any* string you give it, but I didn't try very hard. Or test it.

You can do some typechecking on this AST:
```scala
res10.validate.left.get
// res11: String = repost at position 1.25 is an invalid attribute

```
Ta da! A genuinely meaningful error message.


The first parser only implicitly succeeds or fails. If it throws an error, you know it failed. The second parser is smarter: it represents the possibility of success or failure with a data structure. We're making the implicit explicit. This is reification.

The parser combinators are another example of reification - when you write the parser combinators (see `holophraseparser.scala`) you're building a structure that represents the parsing you want done, rather than doing the parsing. Even though we have to use forbidding words like reification to describe it, this static structure that explicitly represents the computation makes things easier to think about.

In case you're interested in more about parsers, here are two really useful resources I've come across which you might not find easily via google:

1. Chapter 9 in Chiusano and Bjarnasson's "Functional Programming in Scala". It's on writing your own parser combinators. It's superlative technical writing.
2. I've also gotten a lot of utility out of Microsoft's "Monaco" editor. They have a repository where they've defined a whole gaggle of language parsers. They don't use parser combinators, they use this stateful model that they call Monarch, but they have examples of parsing complex languages. I've been looking to them for inspiration on how to build robust parsers. Parsing correct text is simple part. https://github.com/Microsoft/monaco-languages


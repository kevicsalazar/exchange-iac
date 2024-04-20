package cdk

import software.amazon.awscdk.App

fun main() {
    val app = App()
    Stack(app, "ExchangeStack")
    app.synth()
}


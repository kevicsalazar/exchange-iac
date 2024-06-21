package cdk

import cdk.stacks.ExchangeStack
import software.amazon.awscdk.App

fun main() {
    val app = App()
    ExchangeStack(app, "ExchangeStack")
    //ExchangeApiStack(app, "ExchangeApiStack")
    app.synth()
}


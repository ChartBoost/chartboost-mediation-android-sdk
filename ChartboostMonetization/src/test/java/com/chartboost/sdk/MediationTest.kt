package com.chartboost.sdk

import com.chartboost.sdk.internal.Model.MediationBodyFields
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class MediationTest : BehaviorSpec({

    val mediationName = "CBMediationPublic"
    val libraryVersion = "1.2.3"
    val adapterVersion = "3.2.1"

    Given("A mediation with null mediation type") {
        val mediation = Mediation(null, libraryVersion, adapterVersion)
        When("Converting to body fields") {
            val mediationBodyFields = mediation.toMediationBodyFields()
            Then("It should be null") {
                mediationBodyFields.shouldBeNull()
            }
        }
    }

    Given("A mediation with null library version") {
        val mediation = Mediation(mediationName, null, adapterVersion)
        When("Converting to body fields") {
            val mediationBodyFields = mediation.toMediationBodyFields()
            Then("It should have empty library version") {
                mediationBodyFields shouldBe MediationBodyFields("CBMediationPublic", "", adapterVersion)
            }
        }
    }

    Given("A mediation with null adapter version") {
        val expectedMediationName = "CBMediationPublic 1.2.3"
        val mediation = Mediation(mediationName, libraryVersion, null)
        When("Converting to body fields") {
            val mediationBodyFields = mediation.toMediationBodyFields()
            Then("It should have empty adapter version") {
                mediationBodyFields shouldBe MediationBodyFields(expectedMediationName, libraryVersion, "")
            }
        }
    }

    Given("A mediation with standard values") {
        val expectedMediationName = "CBMediationPublic 1.2.3"
        val mediation = Mediation(mediationName, libraryVersion, adapterVersion)
        When("Converting to body fields") {
            val mediationBodyFields = mediation.toMediationBodyFields()
            Then("It should have same values") {
                mediationBodyFields shouldBe MediationBodyFields(expectedMediationName, libraryVersion, adapterVersion)
            }
        }
    }

    Given("A random length mediation name with no spaces") {
        val generator = Arb.string(0..1000).filterNot { it.contains(' ') }
        checkAll(iterations = 100, genA = generator) { mediationName ->
            When("Creating a mediation with this name") {
                val mediation = Mediation(mediationName, libraryVersion, adapterVersion)
                Then("It should truncate mediation name to 50 characters if it's greater than 50") {
                    if (mediationName.length > 50) {
                        mediationName.substring(0 until 50) shouldBe mediation.mediationType
                    } else {
                        mediationName shouldBe mediation.mediationType
                    }
                }
            }
        }
    }

    Given("A mediation random mediation name that could have spaces") {
        val generator = Arb.string(0..50)
        checkAll(iterations = 100, genA = generator) { mediationName ->
            When("Creating a mediation with this name") {
                val mediation = Mediation(mediationName, libraryVersion, adapterVersion)
                Then("It should replace spaces with underscores") {
                    mediation.mediationType shouldBe mediationName.replace(' ', '_')
                }
            }
        }
    }
})

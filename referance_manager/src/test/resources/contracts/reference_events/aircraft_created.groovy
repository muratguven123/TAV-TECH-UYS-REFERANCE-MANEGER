import org.springframework.cloud.contract.spec.Contract

/**
 * reference.events :: AIRCRAFT CREATED
 *
 * businessKey = tailNumber (örn. "TC-JFA")
 * payload = AircraftResponse{id, type, tailNumber}
 */
Contract.make {
    label("triggerAircraftCreated")
    input {
        triggeredBy("triggerAircraftCreated()")
    }
    outputMessage {
        sentTo("reference.events")
        headers {
            header("kafka_messageKey", $(
                producer(regex("AIRCRAFT:.+")),
                consumer("AIRCRAFT:TC-JFA")
            ))
        }
        body([
            entityType  : $(producer(regex("AIRLINE|AIRCRAFT|STATION|ROUTE")), consumer("AIRCRAFT")),
            changeType  : $(producer(regex("CREATED|UPDATED|DELETED")),        consumer("CREATED")),
            businessKey : $(producer(regex(".+")),                              consumer("TC-JFA")),
            payload: [
                id         : $(producer(regex("[0-9]+")), consumer(1)),
                type       : $(producer(regex(".+")),      consumer("B737")),
                tailNumber : $(producer(regex(".+")),      consumer("TC-JFA"))
            ]
        ])
    }
}

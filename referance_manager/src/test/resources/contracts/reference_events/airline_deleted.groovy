import org.springframework.cloud.contract.spec.Contract

/**
 * reference.events :: AIRLINE DELETED
 *
 * DELETED event'lerinde payload null gönderilir.
 * FlightService bu durumda Redis key'ini siler.
 */
Contract.make {
    label("triggerAirlineDeleted")
    input {
        triggeredBy("triggerAirlineDeleted()")
    }
    outputMessage {
        sentTo("reference.events")
        headers {
            header("kafka_messageKey", $(
                producer(regex("AIRLINE:[A-Z]{2}")),
                consumer("AIRLINE:TK")
            ))
        }
        body([
            entityType  : $(producer(regex("AIRLINE|AIRCRAFT|STATION|ROUTE")), consumer("AIRLINE")),
            changeType  : $(producer(regex("CREATED|UPDATED|DELETED")),        consumer("DELETED")),
            businessKey : $(producer(regex("[A-Z]{2}")),                       consumer("TK")),
            payload     : null
        ])
    }
}

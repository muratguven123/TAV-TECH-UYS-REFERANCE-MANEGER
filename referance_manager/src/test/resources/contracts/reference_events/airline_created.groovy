import org.springframework.cloud.contract.spec.Contract

/**
 * reference.events :: AIRLINE CREATED
 *
 * businessKey = IATA kodu (2 büyük harf, örn. "TK")
 * partition key = "AIRLINE:TK"
 * payload = AirlineResponse{id, name, code}
 */
Contract.make {
    label("triggerAirlineCreated")
    input {
        triggeredBy("triggerAirlineCreated()")
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
            changeType  : $(producer(regex("CREATED|UPDATED|DELETED")),        consumer("CREATED")),
            businessKey : $(producer(regex("[A-Z]{2}")),                       consumer("TK")),
            payload: [
                id   : $(producer(regex("[0-9]+")), consumer(1)),
                name : $(producer(regex(".+")),      consumer("Turkish Airlines")),
                code : $(producer(regex("^[A-Z]{2}\$")), consumer("TK"))
            ]
        ])
    }
}

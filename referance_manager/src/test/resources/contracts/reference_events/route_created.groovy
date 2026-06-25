import org.springframework.cloud.contract.spec.Contract

/**
 * reference.events :: ROUTE CREATED
 *
 * businessKey = "<originIcao>-<destinationIcao>" (örn. "LTFM-LTAC")
 * payload = RouteResponse{id, originStation, destinationStation}
 * originStation / destinationStation = StationResponse{id, icaoCode, name}
 */
Contract.make {
    label("triggerRouteCreated")
    input {
        triggeredBy("triggerRouteCreated()")
    }
    outputMessage {
        sentTo("reference.events")
        headers {
            header("kafka_messageKey", $(
                producer(regex("ROUTE:[A-Z]{4}-[A-Z]{4}")),
                consumer("ROUTE:LTFM-LTAC")
            ))
        }
        body([
            entityType  : $(producer(regex("AIRLINE|AIRCRAFT|STATION|ROUTE")), consumer("ROUTE")),
            changeType  : $(producer(regex("CREATED|UPDATED|DELETED")),        consumer("CREATED")),
            businessKey : $(producer(regex("^[A-Z]{4}-[A-Z]{4}\$")),           consumer("LTFM-LTAC")),
            payload: [
                id                : $(producer(regex("[0-9]+")), consumer(1)),
                originStation: [
                    id       : $(producer(regex("[0-9]+")),       consumer(1)),
                    icaoCode : $(producer(regex("^[A-Z]{4}\$")), consumer("LTFM")),
                    name     : $(producer(regex(".+")),            consumer("Istanbul Airport"))
                ],
                destinationStation: [
                    id       : $(producer(regex("[0-9]+")),       consumer(2)),
                    icaoCode : $(producer(regex("^[A-Z]{4}\$")), consumer("LTAC")),
                    name     : $(producer(regex(".+")),            consumer("Esenboga Airport"))
                ]
            ]
        ])
    }
}

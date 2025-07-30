package kafka.entities

final case class Player(id: String,
                        name: String,
                        dob: String,
                        nationality: Option[String],
                        overall: Int,
                        club: String,
                        value: String,
                        foot: String,
                        number: Int,
                        position: String)

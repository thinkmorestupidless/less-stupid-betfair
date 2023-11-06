package gente.oss.betfair.channels.impl.marketdefinitions

import com.thinkmorestupidless.betfair.streams.domain.MarketDefinition
import gente.oss.betfair.channels.impl.marketdefinitions.MarketDefinitionProtocol.Command

object CommandHandler {

  def apply(): (MarketDefinition, Command) => MarketDefinition = ???
}

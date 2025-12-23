package org.wahlen.voucherengine.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class ClockConfig {
    @Bean
    fun systemCock(@Value("\${voucherengine.time.zone:UTC}") zoneId: String): Clock =
        Clock.system(ZoneId.of(zoneId))
}

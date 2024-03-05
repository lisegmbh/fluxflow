package de.lise.fluxflow.springboot.ioc.testservices

import jakarta.annotation.Priority
import org.springframework.stereotype.Service

@Service
@Priority(110)
class ExtendingServiceImpl : ExtendingService
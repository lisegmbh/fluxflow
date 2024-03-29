package de.lise.fluxflow.springboot.configuration

import de.lise.fluxflow.api.bootstrapping.BootstrapAction
import de.lise.fluxflow.api.event.EventService
import de.lise.fluxflow.api.event.FlowListener
import de.lise.fluxflow.api.ioc.IocProvider
import de.lise.fluxflow.api.job.JobService
import de.lise.fluxflow.api.state.ChangeDetector
import de.lise.fluxflow.api.step.StepService
import de.lise.fluxflow.api.workflow.*
import de.lise.fluxflow.engine.bootstrapping.BootstrappingService
import de.lise.fluxflow.engine.continuation.ContinuationService
import de.lise.fluxflow.engine.continuation.history.ContinuationHistoryServiceImpl
import de.lise.fluxflow.engine.event.EventServiceImpl
import de.lise.fluxflow.engine.job.JobActivationService
import de.lise.fluxflow.engine.job.JobSchedulingCallback
import de.lise.fluxflow.engine.job.JobServiceImpl
import de.lise.fluxflow.engine.reflection.ClassLoaderProvider
import de.lise.fluxflow.engine.state.DefaultChangeDetector
import de.lise.fluxflow.engine.step.*
import de.lise.fluxflow.engine.step.action.ActionServiceImpl
import de.lise.fluxflow.engine.step.data.StepDataServiceImpl
import de.lise.fluxflow.engine.step.validation.ValidationService
import de.lise.fluxflow.engine.workflow.*
import de.lise.fluxflow.persistence.continuation.history.ContinuationRecordPersistence
import de.lise.fluxflow.persistence.job.JobPersistence
import de.lise.fluxflow.persistence.step.StepData
import de.lise.fluxflow.persistence.step.StepPersistence
import de.lise.fluxflow.persistence.workflow.WorkflowData
import de.lise.fluxflow.persistence.workflow.WorkflowPersistence
import de.lise.fluxflow.reflection.activation.parameter.IocParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.ParameterResolver
import de.lise.fluxflow.reflection.activation.parameter.PriorityParameterResolver
import de.lise.fluxflow.scheduling.SchedulingCallback
import de.lise.fluxflow.scheduling.SchedulingService
import de.lise.fluxflow.springboot.activation.StepKindMapBuilder
import de.lise.fluxflow.springboot.activation.parameter.SpringValueExpressionParameterResolver
import de.lise.fluxflow.springboot.bootstrapping.SpringBootstrapper
import de.lise.fluxflow.springboot.expression.SpringSelectorExpressionParser
import de.lise.fluxflow.springboot.ioc.SpringIocProvider
import de.lise.fluxflow.stereotyped.continuation.ContinuationBuilder
import de.lise.fluxflow.stereotyped.job.JobDefinitionBuilder
import de.lise.fluxflow.stereotyped.job.parameter.ParameterDefinitionBuilder
import de.lise.fluxflow.stereotyped.metadata.MetadataBuilder
import de.lise.fluxflow.stereotyped.step.StepDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.action.ActionFunctionResolverImpl
import de.lise.fluxflow.stereotyped.step.automation.AutomationDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.DataListenerDefinitionBuilder
import de.lise.fluxflow.stereotyped.step.data.validation.ValidationBuilder
import de.lise.fluxflow.stereotyped.unwrapping.UnwrapService
import de.lise.fluxflow.stereotyped.unwrapping.UnwrapServiceImpl
import de.lise.fluxflow.stereotyped.workflow.ModelListenerDefinitionBuilder
import de.lise.fluxflow.stereotyped.workflow.SelectorExpressionParser
import de.lise.fluxflow.validation.jakarta.JakartaDataValidationBuilder
import de.lise.fluxflow.validation.noop.NoOpDataValidationBuilder
import jakarta.validation.Validator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.*
import org.springframework.expression.spel.standard.SpelExpressionParser
import java.time.Clock

@Configuration
@ComponentScan(basePackages = ["de.lise.fluxflow.springboot.autoconfigure"])
@Import(ChangeDetectionConfiguration::class)
open class BasicConfiguration {
    @Lazy
    @Autowired
    // This needs to be done to avoid the circular dependency between StepServiceImpl and ContinuationService
    private var continuationService: ContinuationService? = null
    
    @Bean
    open fun iocProvider(
        applicationContext: ApplicationContext
    ): IocProvider {
        return SpringIocProvider(applicationContext)
    }
    
    @Bean
    @ConditionalOnMissingBean(Clock::class)
    open fun clock(): Clock {
        return Clock.systemDefaultZone()
    }

    @Bean
    open fun springValueParameterResolver(
        beanFactory: ConfigurableBeanFactory
    ): ParameterResolver {
        return SpringValueExpressionParameterResolver(
            beanFactory
        )
    }

    @Bean
    open fun iocParameterResolver(
        iocProvider: IocProvider
    ): ParameterResolver {
        return IocParameterResolver(iocProvider)
    }

    @Bean
    @Primary
    open fun parameterResolver(
        resolvers: List<ParameterResolver>
    ): ParameterResolver {
        if(resolvers.size == 1) {
            return resolvers.first()
        }
        return PriorityParameterResolver(
            *resolvers.toTypedArray()
        )
    }

    @Bean
    open fun selectorExpressionParser(): SelectorExpressionParser {
        return SpringSelectorExpressionParser(SpelExpressionParser())
    }

    @Bean
    open fun modelListenerDefinitionBuilder(
        parameterResolver: ParameterResolver,
        selectorExpressionParser: SelectorExpressionParser
    ): ModelListenerDefinitionBuilder {
        return ModelListenerDefinitionBuilder(
            parameterResolver,
            DefaultChangeDetector(),
            selectorExpressionParser
        )
    }

    @Bean
    open fun workflowActivationService(
        modelListenerDefinitionBuilder: ModelListenerDefinitionBuilder
    ): WorkflowActivationService {
        return WorkflowActivationServiceImpl(
            modelListenerDefinitionBuilder
        )
    }

    @Bean
    @Primary
    open fun workflowUpdateService(
        persistence: WorkflowPersistence,
        changeDetector: ChangeDetector<WorkflowData>,
        eventService: EventService,
        activationService: WorkflowActivationService
    ): WorkflowUpdateService {
        return WorkflowUpdateServiceImpl(
            persistence,
            changeDetector,
            eventService,
            activationService
        )
    }
   
    @Bean
    @Primary
    open fun workflowQueryService(
        persistence: WorkflowPersistence,
        activationService: WorkflowActivationService
    ): WorkflowQueryServiceImpl {
        return WorkflowQueryServiceImpl(
            persistence,
            activationService
        )
    }

    @Bean
    @Primary
    open fun workflowRemovalService(
        persistence: WorkflowPersistence,
        activationService: WorkflowActivationService,
        stepService: StepServiceImpl,
        jobService: JobServiceImpl,
        eventService: EventService
    ): WorkflowRemovalServiceImpl {
        return WorkflowRemovalServiceImpl(
            persistence,
            activationService,
            stepService,
            jobService,
            eventService
        )
    }

    @Bean
    @Primary
    open fun workflowStarterService(
        persistence: WorkflowPersistence,
        workflowService: WorkflowQueryServiceImpl,
        workflowActivationService: WorkflowActivationService,
        eventService: EventService
    ): WorkflowStarterService {
        return WorkflowStarterServiceImpl(
            persistence,
            workflowActivationService,
            continuationService!!,
            eventService
        )
    }

    @Bean
    open fun workflowService(
        workflowStarterService: WorkflowStarterService,
        workflowQueryService: WorkflowQueryService,
        workflowUpdateService: WorkflowUpdateService,
        workflowRemovalService: WorkflowRemovalService
    ): WorkflowService {
        return WorkflowServiceImpl(
            workflowStarterService,
            workflowQueryService,
            workflowUpdateService,
            workflowRemovalService
        )
    }

    @Bean
    open fun continuationBuilder(): ContinuationBuilder {
        return ContinuationBuilder()
    }

    @Bean
    open fun actionBuilder(
        continuationBuilder: ContinuationBuilder,
        metadataBuilder: MetadataBuilder,
        parameterResolver: ParameterResolver
    ): ActionDefinitionBuilder {
        return ActionDefinitionBuilder(
            continuationBuilder,
            metadataBuilder,
            ActionFunctionResolverImpl(parameterResolver)
        )
    }

    @Bean
    open fun dataListenerDefinitionBuilder(
        continuationBuilder: ContinuationBuilder,
        parameterResolver: ParameterResolver
    ): DataListenerDefinitionBuilder {
        return DataListenerDefinitionBuilder(
            continuationBuilder,
            parameterResolver
        )
    }

    @Bean
    open fun dataBuilder(
        dataListenerDefinitionBuilder: DataListenerDefinitionBuilder,
        validationBuilder: ValidationBuilder,
        metadataBuilder: MetadataBuilder
    ): DataDefinitionBuilder {
        return DataDefinitionBuilder(
            dataListenerDefinitionBuilder,
            validationBuilder,
            metadataBuilder
        )
    }

    @Bean
    open fun automationDefinitionBuilder(
        continuationBuilder: ContinuationBuilder,
        parameterResolver: ParameterResolver
    ): AutomationDefinitionBuilder {
        return AutomationDefinitionBuilder(
            continuationBuilder,
            parameterResolver
        )
    }

    @Bean
    open fun stepMetadataBuilder(): MetadataBuilder {
        return MetadataBuilder()
    }
    
    @Bean
    open fun stepDefinitionBuilder(
        actionDefinitionBuilder: ActionDefinitionBuilder,
        dataDefinitionBuilder: DataDefinitionBuilder,
        metadataBuilder: MetadataBuilder,
        automationDefinitionBuilder: AutomationDefinitionBuilder
    ): StepDefinitionBuilder {
        return StepDefinitionBuilder(
            actionDefinitionBuilder,
            dataDefinitionBuilder,
            metadataBuilder,
            automationDefinitionBuilder
        )
    }

    @Bean
    open fun parameterDefinitionBuilder(): ParameterDefinitionBuilder {
        return ParameterDefinitionBuilder()
    }

    @Bean
    open fun jobDefinitionBuilder(
        parameterDefinitionBuilder: ParameterDefinitionBuilder,
        continuationBuilder: ContinuationBuilder,
        parameterResolver: ParameterResolver,
        metadataBuilder: MetadataBuilder
    ): JobDefinitionBuilder {
        return JobDefinitionBuilder(
            parameterDefinitionBuilder,
            continuationBuilder,
            parameterResolver,
            metadataBuilder,
            mutableMapOf(),
        )
    }

    @Bean
    open fun unwrapService(): UnwrapService {
        return UnwrapServiceImpl()
    }

    @Bean
    open fun classLoaderProvider(
        app: ApplicationContext
    ): ClassLoaderProvider {
        return ClassLoaderProvider {
            app.classLoader!!
        }
    }

    @Bean
    open fun jobActivationService(
        iocProvider: IocProvider,
        jobDefinitionBuilder: JobDefinitionBuilder,
        classLoaderProvider: ClassLoaderProvider
    ): JobActivationService {
        return JobActivationService(
            iocProvider,
            jobDefinitionBuilder,
            classLoaderProvider
        )
    }

    @Bean
    open fun stepActivationService(
        iocProvider: IocProvider,
        stepDefinitionBuilder: StepDefinitionBuilder,
        stepTypeResolver: StepTypeResolver
    ): StepActivationService {
        return StepActivationServiceImpl(
            iocProvider,
            stepDefinitionBuilder,
            stepTypeResolver
        )
    }
    
    @Bean
    open fun stepKindMapBuilder(
        context: ApplicationContext,
        classLoaderProvider: ClassLoaderProvider
    ): StepKindMapBuilder {
        return StepKindMapBuilder(
            context,
            classLoaderProvider.provide()
        )
    }
    
    @Bean
    open fun stepTypeResolver(
        classLoaderProvider: ClassLoaderProvider,
        stepKindMapBuilder: StepKindMapBuilder
    ): StepTypeResolver {
        return StepTypeResolverImpl(
            classLoaderProvider.provide(),
            stepKindMapBuilder.build()
        )
    }

    @Bean
    open fun validationService(
        @Value("\${fluxflow.action.validate-before:true}") validateBeforeActionExecution: Boolean
    ): ValidationService {
        return ValidationService(
            validateBeforeActionExecution
        )
    }

    @Bean
    open fun stepService(
        persistence: StepPersistence,
        stepActivationService: StepActivationService,
        eventService: EventService,
        changeDetector: ChangeDetector<StepData>
    ): StepServiceImpl {
        return StepServiceImpl(
            persistence,
            stepActivationService,
            eventService,
            continuationService!!,
            changeDetector
        )
    }

    @Bean
    open fun continuationService(
        stepServiceImpl: StepServiceImpl,
        activationService: StepActivationService,
        jobService: JobService,
        continuationHistoryService: ContinuationHistoryServiceImpl,
        workflowStarterService: WorkflowStarterService,
        workflowService: WorkflowQueryServiceImpl,
        workflowUpdateService: WorkflowUpdateService,
        workflowRemovalService: WorkflowRemovalServiceImpl
    ): ContinuationService {
        return ContinuationService(
            stepServiceImpl,
            activationService,
            jobService,
            continuationHistoryService,
            workflowStarterService,
            workflowService,
            workflowUpdateService,
            workflowRemovalService
        )
    }

    @Bean
    open fun stepDataService(
        stepServiceImpl: StepServiceImpl,
        workflowUpdateService: WorkflowUpdateService,
        eventService: EventService,
        continuationService: ContinuationService,
        @Value("\${fluxflow.data.allow-inactive-modification:true}") allowInactiveModification: Boolean
    ): StepDataServiceImpl {
        return StepDataServiceImpl(
            stepServiceImpl,
            workflowUpdateService,
            eventService,
            allowInactiveModification,
            continuationService
        )
    }

    @Bean
    open fun actionService(
        workflowUpdateService: WorkflowUpdateService,
        continuationService: ContinuationService,
        validationService: ValidationService,
        eventService: EventService,
    ): ActionServiceImpl {
        return ActionServiceImpl(
            workflowUpdateService,
            continuationService,
            validationService,
            eventService
        )
    }

    @Bean
    open fun jobService(
        jobActivationService: JobActivationService,
        jobPersistence: JobPersistence,
        schedulingService: SchedulingService
    ): JobServiceImpl {
        return JobServiceImpl(
            jobActivationService,
            jobPersistence,
            schedulingService,
        )
    }

    @Bean
    open fun jobSchedulingCallback(
        schedulingService: SchedulingService,
        workflowQueryService: WorkflowQueryService,
        workflowUpdateService: WorkflowUpdateService,
        jobService: JobServiceImpl,
        continuationService: ContinuationService,
    ): SchedulingCallback? {
        val callback = JobSchedulingCallback(
            workflowQueryService,
            workflowUpdateService,
            jobService,
            continuationService,
        )
        schedulingService.registerListener(callback)
        return callback
    }

    @Bean
    open fun eventService(
        listeners: List<FlowListener>
    ): EventServiceImpl {
        return EventServiceImpl(listeners)
    }

    @Bean
    open fun continuationHistoryService(
        continuationRecordPersistence: ContinuationRecordPersistence,
        stepService: StepService,
        clock: Clock,
    ): ContinuationHistoryServiceImpl {
        return ContinuationHistoryServiceImpl(
            continuationRecordPersistence,
            stepService,
            clock
        )
    }

    @Bean
    @ConditionalOnMissingBean(Validator::class)
    open fun noOpValidation(): ValidationBuilder {
        return NoOpDataValidationBuilder()
    }

    @Bean
    @ConditionalOnBean(Validator::class)
    open fun jakartaValidation(validator: Validator): ValidationBuilder {
        return JakartaDataValidationBuilder(validator)
    }

    @Bean
    open fun bootstrappingService(
        actions: List<BootstrapAction>,
        clock: Clock,
    ): BootstrappingService {
        return BootstrappingService(
            actions,
            clock
        )
    }

    @Bean
    open fun springBootstrapper(
        bootstrappingService: BootstrappingService
    ): SpringBootstrapper {
        return SpringBootstrapper(bootstrappingService)
    }
}
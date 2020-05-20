/**
 * Copyright 2009-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.javacrumbs.shedlock.spring.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.concurrent.ScheduledExecutorService;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.DEFAULT_TASK_SCHEDULER_BEAN_NAME;

/**
 * Registers default TaskScheduler if none found.
 * 如果没有TaskScheduler，那就注册一个TaskScheduler
 */
class RegisterDefaultTaskSchedulerPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered, BeanFactoryAware {
    private BeanFactory beanFactory;

    private static final Logger logger = LoggerFactory.getLogger(RegisterDefaultTaskSchedulerPostProcessor.class);

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ListableBeanFactory listableBeanFactory = (ListableBeanFactory) this.beanFactory;
        if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(listableBeanFactory, TaskScheduler.class).length == 0) {
            //如果代码中没有提供线程池
            String[] scheduledExecutorsBeanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(listableBeanFactory, ScheduledExecutorService.class);
            if (scheduledExecutorsBeanNames.length != 1) {
                logger.debug("Registering default TaskScheduler");
                /**
                 * 注册BeanClass为ConcurrentTaskScheduler.class的BeanDefinition，
                 * spring在进行taskScheduler实例化的时候，调用ConcurrentTaskScheduler的默认构造方法
                 * 线程池设置默认的单线程线程池 Executors.newSingleThreadScheduledExecutor()
                  */
                registry.registerBeanDefinition(DEFAULT_TASK_SCHEDULER_BEAN_NAME, rootBeanDefinition(ConcurrentTaskScheduler.class).getBeanDefinition());
                if (scheduledExecutorsBeanNames.length != 0) {
                    logger.warn("Multiple ScheduledExecutorService found, do not know which one to use.");
                }
            } else {
                /**
                 * 当线程池实例只有一个的时候，通过添加propertyReference的方法，把线程池实例添加进TaskScheduler的BeanDefinition
                 * 在AbstractAutowireCapableBeanFactory里面实例化TaskScheduler的时候，
                 * populateBean方法进行属性注入的时候，会先调用PropertyValues pvs = mbd.hasPropertyValues() ? mbd.getPropertyValues() : null;
                 * 如果发现pvs不为空，会根据属性名字进行注入
                 */
                logger.debug("Registering default TaskScheduler with existing ScheduledExecutorService {}", scheduledExecutorsBeanNames[0]);
                registry.registerBeanDefinition(DEFAULT_TASK_SCHEDULER_BEAN_NAME,
                    rootBeanDefinition(ConcurrentTaskScheduler.class)
                        .addPropertyReference("scheduledExecutor", scheduledExecutorsBeanNames[0])
                        .getBeanDefinition()
                );
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}

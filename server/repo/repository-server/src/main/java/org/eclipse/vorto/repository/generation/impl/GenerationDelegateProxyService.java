/**
 * Copyright (c) 2015-2016 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Bosch Software Innovations GmbH - Please refer to git log
 */
package org.eclipse.vorto.repository.generation.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.vorto.repository.api.ModelId;
import org.eclipse.vorto.repository.api.ModelInfo;
import org.eclipse.vorto.repository.api.ModelType;
import org.eclipse.vorto.repository.api.exception.GenerationException;
import org.eclipse.vorto.repository.api.exception.ModelNotFoundException;
import org.eclipse.vorto.repository.api.generation.GeneratedOutput;
import org.eclipse.vorto.repository.api.generation.GeneratorInfo;
import org.eclipse.vorto.repository.api.generation.ServiceClassifier;
import org.eclipse.vorto.repository.core.IModelRepository;
import org.eclipse.vorto.repository.generation.IGeneratorService;
import org.modeshape.common.collection.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author Alexander Edelmann - Robert Bosch (SEA) Pte. Ltd.
 */
@Service
public class GenerationDelegateProxyService implements IGeneratorService {
	
	@Autowired
	private IGeneratorLookupRepository registeredGeneratorsRepository;
	
	@Autowired
	private IModelRepository modelRepositoryService;
	
	
	private RestTemplate restTemplate;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GenerationDelegateProxyService.class);
	
	public GenerationDelegateProxyService() {
		this.restTemplate = new RestTemplate();
	}
	
	@Override
	public void registerGenerator(String serviceKey, String baseUrl, ServiceClassifier classifier) {
		Generator generator = this.getGenerator(serviceKey);
		if (generator == null) {
			this.registeredGeneratorsRepository.save(new Generator(serviceKey, baseUrl, classifier.name()));
		} else {
			generator.setBaseUrl(baseUrl);
			generator.setClassifier(classifier.name());
			this.registeredGeneratorsRepository.save(generator);
		}
	}

	@Override
	public void unregisterGenerator(String serviceKey) {
		Generator generator = getGenerator(serviceKey);
		if (generator != null) {
			this.registeredGeneratorsRepository.delete(generator);
		}
	}

	@Override
	public Set<String> getRegisteredGeneratorServiceKeys(ServiceClassifier classifier) {
		Set<String> serviceKeys = new HashSet<>();
		for (Generator generator : this.registeredGeneratorsRepository.findByClassifier(classifier.name())) {
			serviceKeys.add(generator.getKey());
		}
		return Collections.unmodifiableSet(serviceKeys);
	}

	@Override
	public GeneratorInfo getGeneratorServiceInfo(String serviceKey, boolean includeConfigUI) {
		Generator generatorEntity = getGenerator(serviceKey);
		GeneratorInfo generatorInfo = restTemplate.getForObject(generatorEntity.getGenerationInfoUrl()+"?includeConfigUI={includeConfigUI}", GeneratorInfo.class,includeConfigUI);
		generatorInfo.setInfoUrl(generatorEntity.getGenerationInfoUrl());
		generatorInfo.performRating(generatorEntity.getInvocationCount());		
		return generatorInfo;
	}

	@Override
	public GeneratedOutput generate(ModelId modelId, String serviceKey, Map<String,String> requestParams) {
		ModelInfo modelResource = modelRepositoryService.getById(modelId);
		if (modelResource == null) {
			throw new ModelNotFoundException("Model with the given ID does not exist",null);
		}
		if (modelResource.getType() == ModelType.Datatype || modelResource.getType() == ModelType.Mapping) {
			throw new GenerationException("Provided model is neither an information model nor a function block model!");
		}
		restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
		Generator generatorEntity = getGenerator(serviceKey);
		if (generatorEntity == null) {
			throw new GenerationException("Generator with key "+serviceKey+" is not a registered generator");
		}
		generatorEntity.increaseInvocationCount();
		this.registeredGeneratorsRepository.save(generatorEntity);
		
		ResponseEntity<byte[]> entity = restTemplate.getForEntity(generatorEntity.getGenerationEndpointUrl()+attachRequestParams(requestParams), byte[].class, modelId.getNamespace(), modelId.getName(), modelId.getVersion());
		return new GeneratedOutput(entity.getBody(), extractFileNameFromHeader(entity), entity.getHeaders().getContentLength());
	}
	
	private String attachRequestParams(Map<String, String> requestParams) {
		if (requestParams.isEmpty()) {
			return "";
		} else {
			StringBuilder requestUrlParams = new StringBuilder("?");
			for (Iterator<String> iter = requestParams.keySet().iterator();iter.hasNext();) {
				String key = iter.next();
				requestUrlParams.append(key);
				requestUrlParams.append("=");
				requestUrlParams.append(requestParams.get(key));
				if (iter.hasNext()) {
					requestUrlParams.append("&");
				}
			}
			return requestUrlParams.toString();
		}
	}

	private String extractFileNameFromHeader(ResponseEntity<byte[]> entity) {
		List<String> values = entity.getHeaders().get("content-disposition");
		if (values.size() > 0) {
			int indexOfFileNameStart = values.get(0).indexOf("=");
			return values.get(0).substring(indexOfFileNameStart+1);
		}
		return "generated.output";
	}

	private Generator getGenerator(String serviceKey) {
		List<Generator> generators = this.registeredGeneratorsRepository.findByGeneratorKey(serviceKey);
		if (!generators.isEmpty()) {
			return generators.get(0);
		} else {
			return null;
		}
	}

	@Override
	public Collection<GeneratorInfo> getMostlyUsedGenerators(int top) {
		List<Generator> topResult = new ArrayList<Generator>();

		for (Generator entity : this.registeredGeneratorsRepository.findByClassifier(ServiceClassifier.platform.name())) {
			topResult.add(entity);
		}
		
		topResult.sort(new Comparator<Generator>() {

			@Override
			public int compare(Generator o1, Generator o2) {
				if (o1.getInvocationCount() > o2.getInvocationCount()) {
					return -1;
				} else if (o1.getInvocationCount() < o2.getInvocationCount()) {
					return +1;
				} else {
					return 0;
				}
			}
		});
		
		List<GeneratorInfo> result = new ArrayList<>(top);
		int counter = 0;
		for (Generator entity : topResult) {
			if (counter < top) {
				try {
					result.add(getGeneratorServiceInfo(entity.getKey(),false));
					counter++;
				} catch(Throwable t) {
					LOGGER.warn("Generator " + entity.getKey()+" appears to be offline or not deployed. Skipping...");
				}
				
			}
		}
		
		return result;
	}

}
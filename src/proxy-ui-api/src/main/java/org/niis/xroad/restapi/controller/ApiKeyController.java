/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.domain.PersistentApiKeyType;
import org.niis.xroad.restapi.exceptions.ErrorInfo;
import org.niis.xroad.restapi.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for rest apis for api key operations
 */
@RestController
@RequestMapping("/api/api-key")
@Slf4j
@PreAuthorize("hasRole('XROAD_SYSTEM_ADMINISTRATOR')")
public class ApiKeyController {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    /**
     * create a new api key
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, Object> createKey(@RequestBody List<String> roles) {
        Map.Entry<String, PersistentApiKeyType> createdKeyData = apiKeyRepository.create(roles);
        Map<String, Object> result = new HashMap();
        result.put("key", createdKeyData.getKey());
        result.put("roles", createdKeyData.getValue().getRoles());
        result.put("id", createdKeyData.getValue().getId());
        return result;
    }

    /**
     * list api keys from db - just development time, remove
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Collection<PersistentApiKeyType>> list() {
        Collection<PersistentApiKeyType> keys = apiKeyRepository.listAll();
        return new ResponseEntity<>(keys,
                HttpStatus.OK);
    }

    /**
     * revoke key
     * @param id
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<ErrorInfo> revoke(@PathVariable("id") long id) {
        apiKeyRepository.removeById(id);
        // TO DO: return something else than errorInfo
        return new ResponseEntity<>(new ErrorInfo(HttpStatus.OK.value()),
                HttpStatus.OK);
    }

}

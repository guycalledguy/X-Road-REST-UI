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
package org.niis.xroad.restapi.wsdl;

import ee.ria.xroad.common.SystemProperties;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.exceptions.ErrorDeviation;
import org.niis.xroad.restapi.service.InvalidUrlException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * WsdlValidator as done in X-Road addons: wsdlvalidator
 */
@Slf4j
@Component
public class WsdlValidator {
    // errors

    private String wsdlValidatorCommand;
    private List<String> args;

    public WsdlValidator() {
        wsdlValidatorCommand = SystemProperties.getWsdlValidatorCommand();
    }

    /**
     * validate WSDL with user selected validator
     * @param wsdlUrl
     * @return List of validation warnings that could be ignored by choice
     * @throws WsdlValidatorNotExecutableException when validator is not found or
     * there are errors (not warnings, cant be ignored) when trying to execute the validator
     * @throws InvalidUrlException when wsdl url is missing
     * @throws WsdlValidationFailedException when validation itself fails.
     */
    public List<String> executeValidator(String wsdlUrl)
            throws WsdlValidatorNotExecutableException, WsdlValidationFailedException, InvalidUrlException {
        List<String> warnings = new ArrayList<>();
        // validator not set - this is ok since validator is optional
        if (StringUtils.isEmpty(getWsdlValidatorCommand())) {
            return warnings;
        }
        if (StringUtils.isEmpty(wsdlUrl)) {
            // this is currently a programming error, could just be illegalargumentException
            throw new InvalidUrlException();
        }

        List<String> command = new ArrayList<>();
        command.add(getWsdlValidatorCommand());
        if (args != null && args.size() > 0) {
            command.addAll(args);
        }
        command.add(wsdlUrl);
        Process process;
        ProcessBuilder pb = new ProcessBuilder(command);
        // redirect process errors into process's input stream
        pb.redirectErrorStream(true);
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new WsdlValidatorNotExecutableException(e);
        }

        // gather output into a list of string - needed when returning warnings to the end user
        List<String> processOutput = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            br.lines().forEach(processOutput::add);
        } catch (IOException e) {
            process.destroy();
            throw new WsdlValidatorNotExecutableException(e);
        }

        int exitCode;

        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            // we don't want to throw the InterruptedException from here but we want to retain the interrupted status
            Thread.currentThread().interrupt();
            throw new WsdlValidatorNotExecutableException(e);
        } finally {
            // always destroy the process
            process.destroy();
        }

        // if the validator program fails we attach the validator's output into the exception
        if (exitCode != 0) {
            throw new WsdlValidationFailedException(processOutput);
        } else if (processOutput != null && processOutput.size() > 0) {
            // exitCode was 0 but there were some warnings in the output
            warnings.addAll(processOutput);
        }
        return warnings;
    }

    public String getWsdlValidatorCommand() {
        return wsdlValidatorCommand;
    }

    public void setWsdlValidatorCommand(String wsdlValidatorCommand) {
        this.wsdlValidatorCommand = wsdlValidatorCommand;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    /**
     * Thrown if WSDL validation fails
     */
    public static class WsdlValidationFailedException extends InvalidWsdlException {
        public WsdlValidationFailedException(List<String> metadata) {
            super(metadata);
        }
    }

    /**
     * Thrown if WSDL validation fails
     */
    public static class WsdlValidatorNotExecutableException extends WsdlValidationException {

        public static final String ERROR_WSDL_VALIDATOR_NOT_EXECUTABLE = "wsdl_validator_not_executable";

        public WsdlValidatorNotExecutableException(Throwable t) {
            super(t, new ErrorDeviation(ERROR_WSDL_VALIDATOR_NOT_EXECUTABLE));
        }
    }
}

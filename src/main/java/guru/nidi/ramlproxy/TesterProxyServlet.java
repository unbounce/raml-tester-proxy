/*
 * Copyright (C) 2014 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.ramlproxy;

import guru.nidi.ramltester.MultiReportAggregator;
import guru.nidi.ramltester.RamlDefinition;
import guru.nidi.ramltester.core.RamlReport;
import guru.nidi.ramltester.servlet.ServletRamlRequest;
import guru.nidi.ramltester.servlet.ServletRamlResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.proxy.ProxyServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 */
public class TesterProxyServlet extends ProxyServlet.Transparent {

    private final RamlDefinition ramlDefinition;
    private final MultiReportAggregator aggregator;
    private final Reporter reporter;

    public TesterProxyServlet(String proxyTo, RamlDefinition ramlDefinition, MultiReportAggregator aggregator, Reporter reporter) {
        super(proxyTo.startsWith("http") ? proxyTo : ("http://" + proxyTo), "");
        this.ramlDefinition = ramlDefinition;
        this.aggregator = aggregator;
        this.reporter = reporter;

        aggregator.addReport(new RamlReport(ramlDefinition.getRaml()));
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(new ServletRamlRequest(request), new ServletRamlResponse(response));
    }

    @Override
    protected void onResponseSuccess(HttpServletRequest request, HttpServletResponse response, Response proxyResponse) {
        test(request, response);
        super.onResponseSuccess(request, response, proxyResponse);
    }

    @Override
    protected void onResponseFailure(HttpServletRequest request, HttpServletResponse response, Response proxyResponse, Throwable failure) {
        test(request, response);
        super.onResponseFailure(request, response, proxyResponse, failure);
    }

    private void test(HttpServletRequest request, HttpServletResponse response) {
        test((ServletRamlRequest) request, (ServletRamlResponse) response);
    }

    private void test(ServletRamlRequest request, ServletRamlResponse response) {
        final RamlReport report = ramlDefinition.testAgainst(request, response);
        aggregator.addReport(report);
        reporter.reportViolations(report, request, response);
    }
}

package io.fundrequest.core.request;

import io.fundrequest.core.infrastructure.exception.ResourceNotFoundException;
import io.fundrequest.core.infrastructure.mapping.Mappers;
import io.fundrequest.core.request.command.CreateRequestCommand;
import io.fundrequest.core.request.domain.IssueInformation;
import io.fundrequest.core.request.domain.Request;
import io.fundrequest.core.request.domain.RequestBuilder;
import io.fundrequest.core.request.fund.FundService;
import io.fundrequest.core.request.fund.command.AddFundsCommand;
import io.fundrequest.core.request.infrastructure.RequestRepository;
import io.fundrequest.core.request.infrastructure.github.GithubClient;
import io.fundrequest.core.request.infrastructure.github.parser.GithubParser;
import io.fundrequest.core.request.view.RequestDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
class RequestServiceImpl implements RequestService {

    private RequestRepository requestRepository;
    private Mappers mappers;
    private GithubParser githubLinkParser;
    private FundService fundService;
    private GithubClient githubClient;

    public RequestServiceImpl(RequestRepository requestRepository, Mappers mappers, GithubParser githubLinkParser, FundService fundService, GithubClient githubClient) {
        this.requestRepository = requestRepository;
        this.mappers = mappers;
        this.githubLinkParser = githubLinkParser;
        this.fundService = fundService;
        this.githubClient = githubClient;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestDto> findAll() {
        return mappers.mapList(Request.class, RequestDto.class, requestRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestDto> findAll(Iterable<Long> ids) {
        return mappers.mapList(Request.class, RequestDto.class, requestRepository.findAll(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestDto> findRequestsForUser(Principal principal) {
        return mappers.mapList(Request.class, RequestDto.class, requestRepository.findRequestsForUser(principal.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public RequestDto findRequest(Long id) {
        Request request = findOne(id);
        return mappers.map(Request.class, RequestDto.class, request);
    }

    @Override
    @Transactional
    public RequestDto createRequest(CreateRequestCommand command) {
        Optional<Request> request = requestRepository.findByPlatformAndPlatformId(command.getPlatform(), command.getPlatformId());
        Request r = request.orElseGet(() -> createNewRequest(command));
        fundRequest(command, r);
        return mappers.map(Request.class, RequestDto.class, r);
    }

    private void fundRequest(CreateRequestCommand command, Request r) {
        AddFundsCommand fundsCommand = new AddFundsCommand();
        fundsCommand.setRequestId(r.getId());
        fundsCommand.setAmountInWei(command.getFunds());
        fundService.addFunds(fundsCommand);
    }

    @Override
    @Transactional
    public void addWatcherToRequest(Principal principal, Long requestId) {
        addWatcherToRequest(principal.getName(), findOne(requestId));
    }

    @Override
    @Transactional
    public void removeWatcherFromRequest(Principal principal, Long requestId) {
        removeWatcherFromRequest(principal.getName(), findOne(requestId));
    }

    private Request findOne(Long requestId) {
        return requestRepository.findOne(requestId).orElseThrow(ResourceNotFoundException::new);
    }

    private void addWatcherToRequest(String user, Request r) {
        if (StringUtils.isNotBlank(user)) {
            r.addWatcher(user);
            requestRepository.save(r);
        }
    }

    private void removeWatcherFromRequest(String user, Request r) {
        r.removeWatcher(user);
        requestRepository.save(r);
    }

    private Request createNewRequest(CreateRequestCommand command) {
        IssueInformation issueInformation = githubLinkParser.parseIssue(command.getIssueLink());
        Request request = RequestBuilder.aRequest()
                .withIssueInformation(issueInformation)
                .withTechnologies(getTechnologies(issueInformation))
                .build();
        return requestRepository.save(request);
    }

    private Set<String> getTechnologies(IssueInformation issueInformation) {
        Map<String, Long> languages = githubClient.getLanguages(issueInformation.getOwner(), issueInformation.getRepo());
        return languages.keySet();
    }
}

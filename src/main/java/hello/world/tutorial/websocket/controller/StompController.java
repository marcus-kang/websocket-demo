package hello.world.tutorial.websocket.controller;

import hello.world.tutorial.websocket.dto.ReqDto;
import hello.world.tutorial.websocket.dto.ResDto;
import hello.world.tutorial.websocket.dto.ResSessionsDto;
import hello.world.tutorial.websocket.listener.StompEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Controller
@Slf4j
public class StompController {

    private final TaskScheduler taskScheduler;

    private final StompEventListener eventListener;

    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<String, ScheduledFuture<?>> sessionMap = new ConcurrentHashMap<>();

    public StompController(TaskScheduler taskScheduler, StompEventListener eventListener, SimpMessagingTemplate messagingTemplate) {
        this.taskScheduler = taskScheduler;
        this.eventListener = eventListener;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/hello")
    @SendTo({"/topic/hello", "/topic/hello2"})
    public ResDto basic(ReqDto reqDto, Message<ReqDto> message, MessageHeaders headers){
        log.info("reqDto : {}", reqDto);
        log.info("message : {}", message);
        log.info("headers : {}", headers);

        return new ResDto(reqDto.getMessage().toUpperCase(), LocalDateTime.now());
    }

    @MessageMapping("/code1")
//    @SendTo({"/topic/hello", "/topic/hello2"})
    public void code1(ReqDto reqDto, Message<ReqDto> message, MessageHeaders headers){
        log.info("reqDto : {}", reqDto);
        log.info("message : {}", message);
        log.info("headers : {}", headers);

        ResDto resDto = new ResDto(reqDto.getMessage().toUpperCase(), LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/hello", resDto);

    }

    @MessageMapping("/hello/{detail}")
    @SendTo({"/topic/hello", "/topic/hello2"})
    public ResDto detail(ReqDto reqDto, @DestinationVariable("detail") String detail){
        log.info("reqDto : {}", reqDto);
        log.info("detail : {}", detail);

        return new ResDto(reqDto.getMessage().toUpperCase(), LocalDateTime.now());
    }

    @MessageMapping("/sessions")
    @SendToUser("/queue/sessions")
    public ResSessionsDto sessions(ReqDto reqDto, MessageHeaders headers){
        log.info("reqDto : {}", reqDto);
        String sessionId = headers.get("simpSessionId").toString();
        log.info("sessionId : {}", sessionId);

        Set<String> sessions = eventListener.getSessions();


        return new ResSessionsDto(sessions.size(), sessions.stream().toList(), sessionId, LocalDateTime.now());
    }

    @MessageMapping("/code2")
//    @SendToUser("/queue/sessions")
    public void code2(ReqDto reqDto, MessageHeaders headers){
        log.info("reqDto : {}", reqDto);
        String sessionId = headers.get("simpSessionId").toString();
        log.info("sessionId : {}", sessionId);

        Set<String> sessions = eventListener.getSessions();

        ResSessionsDto resSessionsDto = new ResSessionsDto(sessions.size(), sessions.stream().toList(), sessionId, LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/sessions", resSessionsDto, createHeaders(sessionId));
    }

    MessageHeaders createHeaders(@Nullable String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }

    @MessageMapping("/start")
    public void start(ReqDto reqDto, MessageHeaders headers){

        log.info("header: {}", headers);
        String sessionId = headers.get("simpSessionId").toString();
        log.info("sessionId : {}", sessionId);

        ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleAtFixedRate(() -> {
            Random random = new Random();
            int currentPrice = random.nextInt(100);
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/trade", currentPrice, createHeaders(sessionId));
        }, Duration.ofSeconds(3));

        sessionMap.put(sessionId, scheduledFuture);
    }

    @MessageMapping("/stop")
    public void stop(ReqDto reqDto, MessageHeaders headers){

        log.info("header: {}", headers);
        String sessionId = headers.get("simpSessionId").toString();
        log.info("sessionId : {}", sessionId);

        ScheduledFuture<?> remove = sessionMap.remove(sessionId);
        remove.cancel(true);
    }

    @MessageMapping("/exception")
    @SendTo("/topic/hello")
    public void exception(ReqDto request, MessageHeaders headers) throws Exception {
        log.info("request: {}", request);
        String message = request.getMessage();
        switch(message) {
            case "runtime":
                throw new RuntimeException();
            case "nullPointer":
                throw new NullPointerException();
            case "io":
                throw new IOException();
            case "exception":
                throw new Exception();
            default:
                throw new InvalidParameterException();
        }
    }
}

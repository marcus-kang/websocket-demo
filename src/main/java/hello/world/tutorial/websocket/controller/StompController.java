package hello.world.tutorial.websocket.controller;

import hello.world.tutorial.websocket.dto.ReqDto;
import hello.world.tutorial.websocket.dto.ResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@Slf4j
public class StompController {

    @MessageMapping("/hello")
    @SendTo({"/topic/hello", "/topic/hello2"})
    public ResDto basic(ReqDto reqDto, Message<ReqDto> message, MessageHeaders headers){
        log.info("reqDto : {}", reqDto);
        log.info("message : {}", message);
        log.info("headers : {}", headers);

        return new ResDto(reqDto.getMessage().toUpperCase(), LocalDateTime.now());
    }

    @MessageMapping("/hello/{detail}")
    @SendTo({"/topic/hello", "/topic/hello2"})
    public ResDto detail(ReqDto reqDto, @DestinationVariable("detail") String detail){
        log.info("reqDto : {}", reqDto);
        log.info("detail : {}", detail);

        return new ResDto(reqDto.getMessage().toUpperCase(), LocalDateTime.now());
    }
}

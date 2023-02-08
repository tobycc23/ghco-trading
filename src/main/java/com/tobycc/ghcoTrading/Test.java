package com.tobycc.ghcoTrading;

import com.tobycc.ghcoTrading.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Test {

    @Autowired
    List<Trade> trades;

    public Test() {
        System.out.println();
    }
}

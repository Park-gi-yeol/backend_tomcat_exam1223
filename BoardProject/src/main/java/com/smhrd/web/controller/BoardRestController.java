package com.smhrd.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.web.dto.BoardRequestDTO;
import com.smhrd.web.entity.Board;
import com.smhrd.web.service.BoardService;

import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/api/board")
@CrossOrigin(origins = "http://127.0.0.1:5500/", allowedHeaders = "*")
//@CrossOrigin(origins = "http://10.1.2.6/", allowedHeaders = "*")
public class BoardRestController {
	// 1. r	
	@Autowired
	private BoardService service;   
   @GetMapping("/list")
   public List<Board> list() {
	  return service.getList();
   }
   
   @PostMapping("/register")
   public String register(@ModelAttribute BoardRequestDTO dto) {
	   try {
		   service.register(dto);
		   return "success";
	   } catch(Exception e) {
		   e.printStackTrace();
		   return "fail";
	   }
   }
   
   @GetMapping("/{idx}")
   public Board getDetail(@PathVariable("idx") Long b_idx) {
	   System.out.println("요청한 게시글번호" + b_idx);
	   
	   Board board = service.getDetail(b_idx);
	   
	   System.out.println("조회된 게시글 데이터 : " + board);
	   
	   return board;
   }
   @GetMapping("/{idx}/download")
   public ResponseEntity<Resource> download(@PathVariable("idx") Long idx) {
	   
	   try {
		   return service.download(idx);
	   }catch(Exception e) {
		   e.printStackTrace();
		   return null;
	   }
	   
	   
	   
   }
   
   
}
package com.smhrd.web.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smhrd.web.dto.BoardRequestDTO;
import com.smhrd.web.entity.Board;
import com.smhrd.web.repository.BoardRepository;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public class BoardService {

	@Autowired
	private S3Client s3Client;

	@Value("${ncp.bucket-name}")
	private String bucketName;

	@Value("${file.upload-dir}")
	private String uploadDir;

	@Value("${ncp.end-point}")
	private String endPoint;

//   1. repository 객체 생성
	@Autowired
	private BoardRepository repo;

//   2. DB관련 기능 구현
//   게시글 전체보기 기능
	public List<Board> getList() {
		return repo.findAll();
	}

	// 3. 게시글 작성 기능(파일업로드 포함/로컬저장)
	// - 파일업로드 기능을 구현할 떄, 하나의 Entity로 처리 x
	// - 요청정보를 담는 별도의 DTO 클래스를 만들어서 처리
	// - Entity는 DB와 연동되는 모델 유지
	// - Entity 구조의 노출 위험
	// - 파일 저장 정책(NCP Object Storage, AWS S3)이 변경되었을 때 Entity까지 영향

	public void register(BoardRequestDTO dto) throws IOException {

		// 1. 로컬위치에 저장하는 로직 구현
		String savedPath = null; // 업로드 후 경로 저장 될 변수
		MultipartFile file = dto.getB_file(); // 요청 DTO에 담긴 파일 가져오기 , 업로드한 이미지는 MultipartFile 형태로 받아옴
		System.out.println("멀티파트파일 - " + file);
		// 2. Entity객체를 생성해서 DB저장

		if (file != null && !file.isEmpty()) {
			// 1. 파일명 충돌 방지를 위한 로직구현
			System.out.println("오리지날 파일명 - " + file.getOriginalFilename());
			String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
			// 2. 서버에 저장할 폴더명
			Path uploadFolder = Paths.get(uploadDir);
			System.out.println("uploadDir - " + uploadDir);
			System.out.println("서버에 저장할 폴더명 - " + uploadFolder);

			// 3. 실제 저장될 파일 경로
			Path savePath = uploadFolder.resolve(filename);
			System.out.println("실제저장될 파일 경로" + savePath);
			System.out.println("파일명" + filename);
			// 4. 파일 저장
			Files.copy(file.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

			// 5. DB저장
			savedPath = uploadDir + filename;
			System.out.println(savedPath);

		}
		// 2. Entity 객체를 생성해서 DB저장
//      Board board = new Board();
//      board.setB_title(dto.getB_title());
//      board.setB_writer(dto.getB_writer());
//      board.setB_content(dto.getB_content());
//      board.setB_file_path(savedPath);
//      
//      repo.save(board);

//      
//   }
		// 3. 게시글 작성기능 ( 파일 업로드 / NCP Object)
//   public void register(BoardRequestDto dto) throws Exception {
//      // 1. NCP Object Storage
//      String savedPath = null; // 업로드 후 경로 저장 될 변수
//      MultipartFile file = dto.getB_file(); // 요청 DTO에 담긴 파일 가져오기
//
//      // 2. Entity객체를 생성해서 DB저장
//
//      if (file != null && !file.isEmpty()) {
//         // 1. 파일명 충돌 방지를 위한 로직구현
//         String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
//
//         // 2. NCP에 저장하기 위한 요청 객체 생성
//         PutObjectRequest putObjReq = PutObjectRequest.builder()
//                               .bucket(bucketName)
//                               .key(filename)
//                               .contentType(file.getContentType())
//                               .acl(ObjectCannedACL.PUBLIC_READ)
//                               .build();
//      
//         //3.NCP Object Storage로 전달
//         s3Client.putObject(putObjReq, RequestBody.fromBytes(file.getBytes()));
//         
//         // 4. DB에 저장할 파일 경로 저장
//         savedPath = endPoint + "/" + bucketName + "/" + filename;
//         System.out.println("엔포"+ endPoint);
//         System.out.println("버켓"+bucketName);
//         System.out.println("파일명" + filename);
//         System.out.println(savedPath);
//         https://kr.object.ncloudstorage.com/myapp-test-bucket/d5cc8514-1889-4a46-bb31-fdfa220edcda_rock.png
//         https://kr.object.ncloudstorage.com/myapp-test-bucket/d5cc8514-1889-4a46-bb31-fdfa220edcda_rock.png
//      }
	}

	public Board getDetail(Long b_idx) {
		Optional<Board> optBoard = repo.findById(b_idx);
		if (optBoard.isEmpty()) {
			throw new IllegalArgumentException("게시글이 존재하지 않습니다.");

		}
		return optBoard.get();
	}

	public ResponseEntity<Resource> download(Long b_idx) throws IOException {
		Optional<Board> optBoard = repo.findById(b_idx);
		if (optBoard.isEmpty()) {
			throw new IllegalArgumentException("게시글이 존재하지 않습니다.");
		}
		Board board = optBoard.get();
		String file_path = board.getB_file_path();
		// 파일 경로변수가 null이거나 비워져 있는 경우 404오류를 보내기
		if (file_path == null || file_path.isBlank()) {
			return ResponseEntity.notFound().build();
		}
		System.out.println(file_path);

		// URL에서 파일명 추출
		// URL 구조 : NCP Object Storage의 endpoint+ "/" + bucketName + "/" + fileName(key)
		String marker = "/"+bucketName+"/"; // --> /myapp-obj-sg/ // --> /myapp-obj-sg/
		System.out.println(marker);
		int idx = file_path.indexOf(marker);
		System.out.println(idx);
		String key = file_path.substring(idx + marker.length());
		System.out.println("파일명 => " + key);

//      NCP Object Storage에 저장된 파일을 요청하는 객체 생성
		GetObjectRequest getObjReq = GetObjectRequest.builder().bucket("testapp-obj-sg").key(key).build();
		// 파일 요청하기
		ResponseInputStream<GetObjectResponse> objStream = s3Client.getObject(getObjReq);
		GetObjectResponse meta = objStream.response();

		// 파일 -> byte[]로 변환
      byte[] bytes = objStream.readAllBytes();
      ByteArrayResource resource = new ByteArrayResource(bytes);
//      
		// 다운로드 파일명 :
		// 현재 파일명 : 알파벳 + 숫자조합_파일명.확장자 -> 파일명.확장자

		String filename = key.contains("_") ? key.substring(key.lastIndexOf("_") + 1) : key;
		System.out.println(filename);
		ContentDisposition cd = ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.contentLength(bytes.length)
				.body(resource);
		
	}
}

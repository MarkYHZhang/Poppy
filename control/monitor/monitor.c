#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <unistd.h>
#include <pthread.h>
#include <errno.h>
#include <stdarg.h>

int s_sockfd;

int c_sockfd;
FILE *sockf;
struct sockaddr_in c_addr;
socklen_t c_addr_len;

void (*monitor_cb)(uint32_t type, uint32_t len, uint8_t *data);

uint8_t state;

uint32_t packet_type;
uint32_t packet_len;
uint8_t packet[4096];

uint8_t recv_buf[4096];

int monitor_socket(int port) {
	struct sockaddr_in s_addr;

	s_sockfd = socket(AF_INET, SOCK_STREAM, 0);
	if(s_sockfd < 0) {
		perror("socket");
		return -1;
	}

	if(fcntl(s_sockfd, F_SETFL, O_NONBLOCK) < 0) {
		perror("fcntl");
		return -1;
	}

	if(setsockopt(s_sockfd, SOL_SOCKET, SO_REUSEADDR, (int[]) {1}, sizeof(int)) < 0) {
		perror("setsockopt");
		return -1;
	}

	/*if(setsockopt(s_sockfd, IPPROTO_TCP, TCP_NODELAY, (int[]) {1}, sizeof(int)) < 0) {
		perror("setsockopt");
		return -1;
	}*/

	s_addr.sin_family = AF_INET;
	s_addr.sin_addr.s_addr = INADDR_ANY;
	s_addr.sin_port = htons(port);

	if(bind(s_sockfd, (struct sockaddr *) &s_addr, sizeof(s_addr)) < 0) {
		perror("bind");
		return -1;
	}

	if(listen(s_sockfd, 0) < 0) {
		perror("listen");
		return -1;
	}
	c_addr_len = sizeof(c_addr);

	return 0;
}

void monitor_connection_close() {
	shutdown(c_sockfd, 2);
	close(c_sockfd);
	sockf = NULL;
	c_sockfd = 0;
}

void dispatch_packet() {
	if(monitor_cb != 0) {
		monitor_cb(packet_type, packet_len, packet);
	}
}

void monitor_check() {
	int clientfd = accept(s_sockfd, (struct sockaddr *) &c_addr, &c_addr_len);
	int wouldblock = errno == EAGAIN;
	if(clientfd < 0 && !wouldblock) {
		perror("accept");
		return;
	}

	if(clientfd >= 0) {
		if(fcntl(clientfd, F_SETFL, O_NONBLOCK) < 0) {
			perror("fcntl");
			return;
		}

		if(c_sockfd) {
			monitor_connection_close();
		}

		c_sockfd = clientfd;
		state = 0;
		sockf = fdopen(c_sockfd, "w");
		fprintf(stderr, "Client accepted: %s:%d\n", inet_ntoa(c_addr.sin_addr), c_addr.sin_port);
	}

	if(c_sockfd) {
		ssize_t ret = 0;
		if((ret = read(c_sockfd, recv_buf, sizeof(recv_buf))) > 0) {
			printf("%d\n", ret);
			for(ssize_t i = 0; i < ret; ++i) {
				if(state < 4) {
					*((uint8_t *) &packet_type + state) = recv_buf[i];
					++state;
				} else if(state < 8) {
					*((uint8_t *) &packet_len + state - 4) = recv_buf[i];
					++state;
					if(state == 8) {
						if(packet_len == 0) {
							dispatch_packet();
							state = 0;
						} else if(packet_len >= sizeof(packet)) {
							fprintf(stderr, "Packet length too large: %d\n", packet_len);
							state = 0;
						}
					}
				} else {
					packet[state - 8] = recv_buf[i];
					++state;
					if(state - 8 >= packet_len) {
						state = 0;
						dispatch_packet();
					}
				}
			}
		}

		if(ret <= 0) {
			if(ret != 0) {
				if(errno == EAGAIN) {
					return;
				}
				perror("read");
			}
			monitor_connection_close();
		}
	}
}

void monitor_msg(char *msg, ...) {
	if(sockf != NULL) {
		va_list args;
		va_start(args, msg);

		int ret = vfprintf(sockf, msg, args); 

		va_end(args);

		if(ret >= 0) {
			if(fflush(NULL) < 0 && errno != EAGAIN) {
				perror("fflush");
				monitor_connection_close();
			}
			return;
		}

		monitor_connection_close();
	}
}

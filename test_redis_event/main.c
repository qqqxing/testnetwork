/*
 * main.c
 *
 *  Created on: Nov 16, 2012
 *      Author: jack
 */
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

#include "anet.h"
#include "ae.h"
#include "main.h"

typedef struct temp_server {
	int ipfd;
	char neterr[256];
	int port;
	char *bindaddr;
	aeEventLoop * el;
} Nserver;

char *local_ip = "127.0.0.1";
Nserver server;

void oom(const char *msg) {
	printf("%s: Out of memory\n", msg);
	sleep(1);
	abort();
}


void free_buffer(buffer * b){
	free(b->buf);
	free(b);
}


void sendReplyToClient(aeEventLoop *el, int fd, void *privdata, int mask) {
	redisClient *c = privdata;
	int nwritten = 0;

	buffer *p, *q,*f;
	p = c->head;
	while (p != NULL) {
		q = p;

		int flag = -1;
		while (1) {
			nwritten
					= write(fd, q->buf + q->writepos, q->buf_len - q->writepos);
			if (nwritten <= 0) {
				if (nwritten == -1) {
					if (errno == EAGAIN) {
						nwritten = 0;
						flag = 0;
						break;
					} else {
						printf("Error writing to client: %s\n", strerror(errno));
						freeClient(c);
						return;
					}
				}
				flag = 1;
				break;
			}

			q->writepos += nwritten;
			c->total_write += nwritten;
			if(q->writepos >= q->buf_len){
				//write finished.
				flag = 2;
				break ;
			}
		}


		//flag == 0 ,write EAGAIN
		//flag == 2, write finished.
		if(flag == 2){
			f = p;
			p = q->next;
			c->head = p;
			if(c->head == NULL){
				c->tail = NULL;
				c->writting = 0;
				aeDeleteFileEvent(server.el,c->fd,AE_WRITABLE);
			}
			free_buffer(f);
		}else{
			// EAGAIN
			break;
		}
	}
}


/* Set the event loop to listen for write events on the client's socket.
 * Typically gets called every time a reply is built. */
int _installWriteEvent(redisClient *c) {
	if (c->fd <= 0)
		return -1;
	if (!c->writting) {
		if (aeCreateFileEvent(server.el, c->fd, AE_WRITABLE, sendReplyToClient,
				c) == AE_ERR) {
			return 0;
		}
		c->writting = 1;
		return 1;
	}
	return 1;
}

void addReply(redisClient *c) {
	_installWriteEvent(c);
}

void freeClient(redisClient *c) {
	aeDeleteFileEvent(server.el, c->fd, AE_READABLE);

	aeDeleteFileEvent(server.el, c->fd, AE_WRITABLE);
	printf("Socket : %d is closed.\n", c->fd);
	printf("Socket total read %d bytes\n",c->total_read);
	printf("Socket total write %d bytes\n",c->total_write);
	close(c->fd);
}

void add_buffer_to_client(redisClient * c, unsigned char * buf, int len) {
	unsigned char * new_read = (unsigned char *) malloc(len);
	memcpy(new_read, buf, len);

	buffer * b = (buffer *) malloc(sizeof(buffer));
	b->buf = new_read;
	b->buf_len = len;
	b->writepos = 0;
	b->next = NULL;

	if (c->head == NULL && c->tail == NULL) {
		//empty list
		c->head = b;
		c->tail = b;
	} else {
		// add to tail
		c->tail->next = b;
		c->tail = b;
	}

}

void readQueryFromClient(aeEventLoop *el, int fd, void *privdata, int mask) {
	redisClient *c = (redisClient*) privdata;
	char buf[REDIS_IOBUF_LEN];
	int nread;
	REDIS_NOTUSED(el);
	REDIS_NOTUSED(mask);
	nread = read(fd, buf, REDIS_IOBUF_LEN);

	if (nread == -1) {
		if (errno == EAGAIN) {
			nread = 0;
		} else {
			printf("Reading from client: %s\n", strerror(errno));
			freeClient(c);
			return;
		}
	} else if (nread == 0) {
		freeClient(c);
		return;
	}
//	printf("[Read length]: %d\n", nread);
	add_buffer_to_client(c, buf, nread);
	c->total_read += nread;

	addReply(c);
}

redisClient *createClient(int fd) {
	redisClient *c = zmalloc(sizeof(redisClient));

	anetNonBlock(NULL, fd);
	anetTcpNoDelay(NULL, fd);
	if (aeCreateFileEvent(server.el, fd, AE_READABLE, readQueryFromClient, c)
			== AE_ERR) {
		close(fd);
		zfree(c);
		return NULL;
	}
	c->fd = fd;
	c->head = NULL;
	c->tail = NULL;
	c->writting = 0;
	c->total_read = 0;
	c->total_write = 0;
	return c;
}

static void acceptCommonHandler(int fd) {
	redisClient *c;
	if ((c = createClient(fd)) == NULL) {
		printf("Error allocating resoures for the client\n");
		close(fd); /* May be already closed, just ingore errors */
		return;
	}

}

void acceptTcpHandler(aeEventLoop *el, int fd, void *privdata, int mask) {
	int cport, cfd;
	char cip[128];
	REDIS_NOTUSED(el);
	REDIS_NOTUSED(mask);
	REDIS_NOTUSED(privdata);
	char neterr[256];

	cfd = anetTcpAccept(neterr, fd, cip, &cport);
	if (cfd == AE_ERR) {
		printf("Accepting client connection: %s\n", neterr);

		return;
	}
	printf("Accepted %s:%d\n", cip, cport);
	acceptCommonHandler(cfd);
}

int main(int argc, char **argv) {

	printf("Hello there!\n");

	server.port = 9000;
	server.bindaddr = local_ip;
	memset(server.neterr, 0, 256);

	server.ipfd = anetTcpServer(server.neterr, server.port, server.bindaddr);
	if (server.ipfd == ANET_ERR) {
		printf("Opening port %d: %s\n", server.port, server.neterr);
		exit(1);
	}

	printf("Opening port %d success\n", server.port);

	/* Create event loop */

	server.el = aeCreateEventLoop();

	if (server.ipfd > 0 && aeCreateFileEvent(server.el, server.ipfd,
			AE_READABLE, acceptTcpHandler, NULL) == AE_ERR)
		oom("creating file event");

	printf("Going into event loop...\n");
	aeMain(server.el);

	/* Delete event loop */
	aeDeleteEventLoop(server.el);

	return 0;
}


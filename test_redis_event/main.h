/*
 * main.h
 *
 *  Created on: Nov 16, 2012
 *      Author: jack
 */

#ifndef MAIN_H_
#define MAIN_H_

#define REDIS_IOBUF_LEN         (1024*16)

/* Anti-warning macro... */
#define REDIS_NOTUSED(V) ((void) V)

typedef struct a_buf {
	unsigned char * buf;
	int buf_len;
	int writepos;
	struct a_buf * next;
} buffer;

typedef struct rclient {
	int fd;
	buffer * head;
	buffer * tail;

	int writting;
	long total_read;
	long total_write;
} redisClient;

#endif /* MAIN_H_ */

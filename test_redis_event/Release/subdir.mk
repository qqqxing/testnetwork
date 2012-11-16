################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../ae.c \
../ae_epoll.c \
../anet.c \
../main.c \
../networking.c \
../zmalloc.c 

OBJS += \
./ae.o \
./ae_epoll.o \
./anet.o \
./main.o \
./networking.o \
./zmalloc.o 

C_DEPS += \
./ae.d \
./ae_epoll.d \
./anet.d \
./main.d \
./networking.d \
./zmalloc.d 


# Each subdirectory must supply rules for building sources it contributes
%.o: ../%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	gcc -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '



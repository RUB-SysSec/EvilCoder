package tools.data_flow;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

class Init_glibc_data_trans
 {
   public static HashMap<String, HashMap<Long, List<Long>>> sets_param = null;
   public static HashMap<String, List<Pair<Long, List<Long>>>> data_trans = null;

   private static List<Long> list_helper(int ... args)
    {
    List<Long> ret = new ArrayList<>();
      for(int arg : args)
       {
        ret.add(new Long(arg));
       }
     return ret;
    }


   private static void sets_param_helper(String func_name, List<Long> values)
    {
      if(!sets_param.containsKey(func_name))
       {
        sets_param.put(func_name, new HashMap<Long, List<Long>>());
       }
     sets_param.get(func_name).put(new Long(values.size()), values);
    }

   private static void data_trans_helper(String func_name, int arg_index, List<Long> values)
    {
      if(!data_trans.containsKey(func_name))
       {
        data_trans.put(func_name, new ArrayList<Pair<Long, List<Long>>>());
       }
     data_trans.get(func_name).add(new Pair<Long, List<Long>>(new Long(arg_index), values));
    }


   public static void main(String[] argv)
    {
     sets_param = new HashMap<>();
     data_trans = new HashMap<>();

    // Nothing happens
    List<String> nothing_happens = Arrays.asList("abort abs access alloca atof atoi atol bind calloc ceil cos error exit exp fabs fclose fcntl fdopen feof ferror fflush fgetc fileno floor fopen fork fprintf fputc fputs free fseek ftell fwrite getc getenv getopt getpid gettext getuid htonl htons index int sleep isalnum isalpha isdigit isspace localtime log lseek malloc memcmp mmap ntohl ntohs perror pow printf putc putchar puts rand random realloc remove send setlocale setsockopt signal sin sqrt strcasecmp strchr strcmp strdup strerror strlen strncasecmp strncmp strrchr strstr strtok syslog system tolower toupper unlink warn".split(" ", -1));

      for(String it : nothing_happens)
       {
       List<Long> the_list = new ArrayList<>();
         for(Long i = new Long(1); i<10; ++i)
          {
           the_list.add(new Long(-1));
           sets_param_helper(it, new ArrayList<Long>(the_list));
          }
       }

//uid_t getuid(void);
//ssize_t send(int sockfd, const void *buf, size_t len, int flags);
//int setsockopt(int sockfd, int level, int optname, const void *optval, socklen_t optlen);
//pid_t fork(void);
//void *mmap(void *addr, size_t length, int " prot ", int " flags, int fd, off_t offset);
//int access(const char *pathname, int mode);
//off_t lseek(int fd, off_t offset, int whence); 
//int bind(int sockfd, const struct sockaddr *addr, socklen_t addrlen);
//int fcntl(int fd, int cmd, ... /* arg */ );
//pid_t getpid(void);
//sighandler_t signal(int signum, sighandler_t handler); 
//int unlink(const char *pathname);
//char * gettext (const char * msgid);
//char *getenv(const char *name);
//char *index(const char *s, int c);
//char *setlocale(int category, const char *locale);
//char *strchr(const char *s, int c);
//char *strdup(const char *s);
//char *strerror(int errnum);
//char *strrchr(const char *s, int c);
//char *strstr(const char *haystack, const char *needle);
//char *strtok(char *str, const char *delim);
//double atof(const char *nptr);
//double ceil(double x);
//double cos(double x);
//double exp(double x);
//double fabs(double x);
//double floor(double x);
//double log(double x);
//double pow(double x, double y);
//double sin(double x);
//double sqrt(double x);
//FILE *fdopen(int fd, const char *mode);
//FILE *fopen(const char *path, const char *mode);
//int abs(int j);
//int atoi(const char *nptr);
//int fclose(FILE *fp);
//int feof(FILE *stream);
//int ferror(FILE *stream);
//int fflush(FILE *stream);
//int fgetc(FILE *stream);
//int fileno(FILE *stream);
//int fprintf(FILE *stream, const char *format, ...);
//int fputc(int c, FILE *stream);
//int fputs(const char *s, FILE *stream);
//int fseek(FILE *stream, long offset, int whence);
//int getc(FILE *stream);
//int getopt(int argc, char * const argv[], const char *optstring);
//int isalnum(int c);
//int isalpha(int c);
//int isdigit(int c);
//int isspace(int c);
//int memcmp(const void *s1, const void *s2, size_t n);
//int printf(const char *format, ...);
//int putchar(int c);
//int putc(int c, FILE *stream);
//int puts(const char *s);
//int rand(void);
//int remove(const char *pathname);
//int strcasecmp(const char *s1, const char *s2);
//int strcmp(const char *s1, const char *s2);
//int strncasecmp(const char *s1, const char *s2, size_t n);
//int strncmp(const char *s1, const char *s2, size_t n);
//int system(const char *command);
//int tolower(int c);
//int toupper(int c);
//long atol(const char *nptr);
//long ftell(FILE *stream);
//long int random(void);
//size_t fwrite(const void *ptr, size_t size, size_t nmemb, FILE *stream);
//size_t strlen(const char *s);
//struct tm *localtime(const time_t *timep);
//uint16_t htons(uint16_t hostshort);
//uint16_t ntohs(uint16_t netshort);
//uint32_t htonl(uint32_t hostlong);
//uint32_t ntohl(uint32_t netlong);
//unsigned int sleep(unsigned int seconds);
//void abort(void);
//void *alloca(size_t size);
//void *calloc(size_t nmemb, size_t size);
//void exit(int status);
//void free(void *ptr);
//void *malloc(size_t size);
//void perror(const char *s);
//void *realloc(void *ptr, size_t size);
//void syslog(int priority, const char *format, ...);
//void warn(const char *fmt, ...);
//void error(int status, int errnum, const char *format, ...);





//Dunno
//int select(int nfds, fd_set *readfds, fd_set *writefds, fd_set *exceptfds, struct timeval *timeout);
     sets_param_helper("select", list_helper(-1, -1, -1, -1, -1));


// Sets something
//data_trans[""] = [(,[])]
//sets_param[""][] = []
//     data_trans_helper("", , list_helper());
//pid_t wait(int *status);
     data_trans_helper("wait", 0, list_helper(-1));
     sets_param_helper("wait", list_helper(1));
//int dup2(int oldfd, int newfd);
     data_trans_helper("dup2", 1, list_helper(0));
     sets_param_helper("dup2", list_helper(0, 1));
//int sigaction(int signum, const struct sigaction *act, struct sigaction *oldact);
     data_trans_helper("sigaction", 2, list_helper(1));
     sets_param_helper("sigaction", list_helper(1, -1, 1));
//int fstat(int fd, struct stat *buf);
     data_trans_helper("fstat", 1, list_helper(-1));
     sets_param_helper("fstat", list_helper(1, 1));
//int gettimeofday(struct timeval *tv, struct timezone *tz);
     data_trans_helper("gettimeofday", 0, list_helper(-1));
     data_trans_helper("gettimeofday", 1, list_helper(-1));
     sets_param_helper("gettimeofday", list_helper(1, 1));
//int accept(int sockfd, struct sockaddr *addr, socklen_t *addrlen);
     data_trans_helper("accept", 1, list_helper(-1));
     data_trans_helper("accept", 2, list_helper(-1));
     sets_param_helper("accept", list_helper(1, 1, 1));
//char *fgets(char *s, int size, FILE *stream);
     data_trans_helper("fgets", 0, list_helper(-1));
     sets_param_helper("fgets", list_helper(1, -1, -1));
//char *strcat(char *dest, const char *src);
     data_trans_helper("strcat", 0, list_helper(1));
     sets_param_helper("strcat", list_helper(1, -1));
//char *strcpy(char *dest, const char *src);
     data_trans_helper("strcpy", 0, list_helper(1));
     sets_param_helper("strcpy", list_helper(1, -1));
//char *strncpy(char *dest, const char *src, size_t n);
     data_trans_helper("strncpy", 0, list_helper(1));
     sets_param_helper("strncpy", list_helper(1, -1, -1));
//int snprintf(char *str, size_t size, const char *format, ...);
     data_trans_helper("snprintf", 0, list_helper(3, 4, 5, 6, 7, 8, 9));
     sets_param_helper("snprintf", list_helper(1, -1, -1, -1));
     sets_param_helper("snprintf", list_helper(1, -1, -1, -1, -1));
     sets_param_helper("snprintf", list_helper(1, -1, -1, -1, -1, -1));
     sets_param_helper("snprintf", list_helper(1, -1, -1, -1, -1, -1, -1));
     sets_param_helper("snprintf", list_helper(1, -1, -1, -1, -1, -1, -1, -1));
     sets_param_helper("snprintf", list_helper(1, -1, -1, -1, -1, -1, -1, -1, -1));
//int sprintf(char *str, const char *format, ...);
     data_trans_helper("sprintf", 0, list_helper(2, 3, 4, 5, 6, 7, 8, 9));
     sets_param_helper("sprintf", list_helper(1, -1, -1));
     sets_param_helper("sprintf", list_helper(1, -1, -1, -1));
     sets_param_helper("sprintf", list_helper(1, -1, -1, -1, -1));
     sets_param_helper("sprintf", list_helper(1, -1, -1, -1, -1, -1));
     sets_param_helper("sprintf", list_helper(1, -1, -1, -1, -1, -1, -1));
     sets_param_helper("sprintf", list_helper(1, -1, -1, -1, -1, -1, -1, -1));
     sets_param_helper("sprintf", list_helper(1, -1, -1, -1, -1, -1, -1, -1, -1));
//int sscanf(const char *str, const char *format, ...);
     data_trans_helper("sscanf", 2, list_helper(0));
     data_trans_helper("sscanf", 3, list_helper(0));
     data_trans_helper("sscanf", 4, list_helper(0));
     data_trans_helper("sscanf", 5, list_helper(0));
     data_trans_helper("sscanf", 6, list_helper(0));
     data_trans_helper("sscanf", 7, list_helper(0));
     data_trans_helper("sscanf", 8, list_helper(0));
     data_trans_helper("sscanf", 9, list_helper(0));

     sets_param_helper("sscanf", list_helper(1, -1, 1));
     sets_param_helper("sscanf", list_helper(1, -1, 1, 1));
     sets_param_helper("sscanf", list_helper(1, -1, 1, 1, 1));
     sets_param_helper("sscanf", list_helper(1, -1, 1, 1, 1, 1));
     sets_param_helper("sscanf", list_helper(1, -1, 1, 1, 1, 1, 1));
     sets_param_helper("sscanf", list_helper(1, -1, 1, 1, 1, 1, 1, 1));
     sets_param_helper("sscanf", list_helper(1, -1, 1, 1, 1, 1, 1, 1, 1));
//long int strtol(const char *nptr, char **endptr, int base);
     data_trans_helper("strtol", 1, list_helper(-1));
     sets_param_helper("strtol", list_helper(1, 1, -1));
//size_t fread(void *ptr, size_t size, size_t nmemb, FILE *stream);
     data_trans_helper("fread", 0, list_helper(-1));
     sets_param_helper("fread", list_helper(1, -1, -1, -1));
//unsigned long int strtoul(const char *nptr, char **endptr, int base);
     data_trans_helper("strtoul", 1, list_helper(-1));
     sets_param_helper("strtoul", list_helper(1, 1, -1));
//void bzero(void *s, size_t n);
     data_trans_helper("bzero", 0, list_helper(-1));
     sets_param_helper("bzero", list_helper(1, -1));
//void *memcpy(void *dest, const void *src, size_t n);
     data_trans_helper("memcpy", 0, list_helper(1));
     sets_param_helper("memcpy", list_helper(1, -1, -1));
//void *memmove(void *dest, const void *src, size_t n);
     data_trans_helper("memmove", 0, list_helper(1));
     sets_param_helper("memmove", list_helper(1, -1, -1));
//void *memset(void *s, int c, size_t n);
     data_trans_helper("memset", 0, list_helper(1));
     sets_param_helper("memset", list_helper(1, -1, -1));
//int fscanf(FILE *stream, const char *format, ...);
     data_trans_helper("fscanf", 2, list_helper(0));
     data_trans_helper("fscanf", 3, list_helper(0));
     data_trans_helper("fscanf", 4, list_helper(0));
     data_trans_helper("fscanf", 5, list_helper(0));
     data_trans_helper("fscanf", 6, list_helper(0));
     data_trans_helper("fscanf", 7, list_helper(0));
     data_trans_helper("fscanf", 8, list_helper(0));
     data_trans_helper("fscanf", 9, list_helper(0));

     sets_param_helper("fscanf", list_helper(1, -1, 1));
     sets_param_helper("fscanf", list_helper(1, -1, 1, 1));
     sets_param_helper("fscanf", list_helper(1, -1, 1, 1, 1));
     sets_param_helper("fscanf", list_helper(1, -1, 1, 1, 1, 1));
     sets_param_helper("fscanf", list_helper(1, -1, 1, 1, 1, 1, 1));
     sets_param_helper("fscanf", list_helper(1, -1, 1, 1, 1, 1, 1, 1));
     sets_param_helper("fscanf", list_helper(1, -1, 1, 1, 1, 1, 1, 1, 1));



    HashMap<String, Object> data = new HashMap<>();
     data.put("sets_param", sets_param);
     data.put("data_trans", data_trans);
     Pickle.save_to_file("data_trans.ser", data);
    } // EOF main

 } // EOF class

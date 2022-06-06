#!/bin/bash

echo Unzip zstd...
tar zxvf zstd*.tar.gz &>/dev/null

zstdDir=$(find . -maxdepth 1 -name "zstd*" -type d)

echo Compiling zstd...
cd $zstdDir
make -j8 &>/dev/null
echo Compiling complete!

cd ..

total=0
success=0
error=0
if [ -e log ];then
    rm -rf log
fi
for i in $(find -name "*.tar*" -type f)
do
    let total+=1
    case ${i##*.} in
        tar)
            tar -t -f $i &>/dev/null
            if [[ $? == 0 ]];then
                let success+=1
                echo Success
            else
                let error+=1
                echo Error
                echo $i >> log
            fi
        ;;
        zst | lz4)
            ./$zstdDir/zstd -t $i &>/dev/null
            if [[ $? == 0 ]];then
                let success+=1
                echo Success
            else
                let error+=1
                echo Error
                echo $i >> log
            fi
        ;;
        *) let total-=1 ;;
    esac
done
echo Total:$total
echo Success:$success
echo Error:$error

if [ -e log ];then
    echo "Broken files:"
    cat log
    rm -rf log
fi
rm -rf $zstdDir
# MvRx Simplify

This is a simplified version of MvRx. The API is mostly the same as MvRx.

> modification points

1. Remove initialState creation reflection call

2. Remove ViewModel creation reflection call

3. Remove RxJava
    
    ~~~
    Async no longer has the asynchronous function
    ~~~
    
4. Remove the synchronize annotations and provide a `postState` method

5. Use the SavedStateHandle for state saving and recovery

6. Optimize the method of attribute observation

7. Remove BaseMvRxFragment
